/*
 * Copyright (c) 2012 Stefan Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.depgraph_view.model.graph;

import com.google.common.collect.Sets;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.BuildStep;
import hudson.tasks.Publisher;
import java.util.ArrayList;
import java.util.Arrays;
import org.jenkins_ci.plugins.flexible_publish.ConditionalPublisher;
import org.jenkins_ci.plugins.flexible_publish.FlexiblePublisher;

/**
 * Provides {@link SubProjectEdge}s
 */
public class SubProjectEdgeProvider implements EdgeProvider {

    private final boolean isFlexiblePublishPluginInstalled;

    @Inject
    public SubProjectEdgeProvider(Jenkins jenkins) {
        isFlexiblePublishPluginInstalled = jenkins.getPlugin("flexible-publish") != null;
    }

    @Override
    public Iterable<Edge> getEdgesIncidentWith(AbstractProject<?, ?> project) {
        Set<Edge> subprojectEdges = Sets.newHashSet();

        if(project instanceof FreeStyleProject) {

            FreeStyleProject proj = (FreeStyleProject) project;
            List<Builder> builders;
            if(isFlexiblePublishPluginInstalled){
                List<Publisher> publishers = proj.getPublishersList();
                ArrayList<Builder> tmpBuilders = new ArrayList<Builder>();
                tmpBuilders.addAll(proj.getBuilders());
                for (Publisher publisher : publishers){
                    if(publisher instanceof FlexiblePublisher) {
                        List<ConditionalPublisher> conditionalpublishers =
                                ((FlexiblePublisher)publisher).getPublishers();
                        for(ConditionalPublisher p : conditionalpublishers){
                            List<BuildStep> steps = p.getPublisherList();
                            for (BuildStep step : steps){
                                if(step instanceof TriggerBuilder){
                                    tmpBuilders.add((TriggerBuilder)step);
                                }
                            }
                        }
                    }
                }
                builders = tmpBuilders;
            } else {
                builders = proj.getBuilders();
            }

            for (Builder builder : builders) {

                if (builder instanceof TriggerBuilder) {

                    TriggerBuilder tBuilder = (TriggerBuilder) builder;
                    List<BlockableBuildTriggerConfig> configs =  tBuilder.getConfigs();
                    for (BlockableBuildTriggerConfig config : configs){
                       List<String> projects = Arrays.asList(config.getProjects().split(","));
                       Jenkins jenkins = Jenkins.getInstance();
                       for(String projectName : projects){
                            AbstractProject<?,?> projectFromName = jenkins.getItem(projectName.trim(), project.getParent(), AbstractProject.class);

                            if (projectFromName != null) {
                                subprojectEdges.add(
                                        new SubProjectEdge(node(project),node(projectFromName)));
                            }
                       }
                    }
                }
            }
        }
        return subprojectEdges;
    }
}
