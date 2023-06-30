/** 
 * Copyright (C) 2021 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.bonitasoft.plugin.analyze.report.model.Issue;

@Named
@Singleton
public class IncompatibleDependencyValidator implements DependencyValidator {

    /**
     * List of dependencies already provided in Bonita runtime that should not be found in the runtime dependency tree.
     */
    private static final Set<String> CONFLICTING_ARTIFACTS = Set.of(
            "org.codehaus.groovy:groovy-all:jar",
            "org.bonitasoft.engine:bonita-server:jar",
            "com.bonitasoft.engine:bonita-server-sp:jar",
            "org.bonitasoft.engine:bonita-client:jar",
            "com.bonitasoft.engine:bonita-client-sp:jar",
            "org.bonitasoft.engine:bonita-common:jar",
            "com.bonitasoft.engine:bonita-common-sp:jar",
            "org.bonitasoft.web:bonita-web-extensions:jar",
            "com.bonitasoft.web:bonita-web-extensions-sp:jar");

    private DependencyGraphBuilder dependencyGraphBuilder;

    @Inject
    public IncompatibleDependencyValidator(DependencyGraphBuilder dependencyGraphBuilder) {
        this.dependencyGraphBuilder = dependencyGraphBuilder;
    }

    private static boolean isConflictingArtifact(Artifact artifact) {
        return CONFLICTING_ARTIFACTS
                .contains(String.format("%s:%s:%s",
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getType()));
    }

    private String rootNodeId(DependencyNode node, String projectArtifactId) {
        var rootNode = node;
        while (!Objects.equals(rootNode.getParent().getArtifact().getId(), projectArtifactId)) {
            rootNode = rootNode.getParent();
        }
        return rootNode.getArtifact().getId();
    }

    @Override
    public List<Issue> validate(MavenProject project, ProjectBuildingRequest projectBuildingRequest)
            throws MojoExecutionException {
        try {
            var dependencyGraph = dependencyGraphBuilder.buildDependencyGraph(projectBuildingRequest,
                    new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME));

            List<DependencyNode> conflictingNodes = new ArrayList<>();
            dependencyGraph.accept(new CollectingDependencyNodeVisitor() {

                @Override
                public boolean visit(DependencyNode node) {
                    if (IncompatibleDependencyValidator.isConflictingArtifact(node.getArtifact())) {
                        conflictingNodes.add(node);
                    }
                    return super.visit(node);
                }
            });

            return conflictingNodes.stream()
                    .map(node -> createIssue(node, project.getArtifact().getId()))
                    .collect(Collectors.toList());
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException("Failed to build dependency graph", e);
        }
    }

    private Issue createIssue(DependencyNode node, String projectArtifactId) {
        var nodeId = node.getArtifact().getId();
        var rootId = rootNodeId(node, projectArtifactId);
        if (Objects.equals(rootId, nodeId)) {
            return Issue.create(
                    Issue.Type.INCOMPATIBLE_DEPENDENCY,
                    String.format("%s is conflicting with Bonita provided dependencies.",
                            nodeId),
                    Issue.Severity.ERROR,
                    nodeId);
        } else {
            return Issue.create(
                    Issue.Type.INCOMPATIBLE_DEPENDENCY,
                    String.format(
                            "%s depends on %s which is conflicting with Bonita provided dependencies.",
                            rootId,
                            nodeId),
                    Issue.Severity.ERROR,
                    rootId,
                    nodeId);
        }
    }

}
