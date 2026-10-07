/** 
 * Copyright (C) 2026 BonitaSoft S.A.
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
package org.bonitasoft.plugin.build.bar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.eclipse.aether.graph.Dependency;

/**
 * Resolves and copies the runtime-classpath jar dependencies of a (generated) pom file, in-process,
 * without forking an external Maven build. This mirrors what the {@code dependency:copy-dependencies}
 * goal with {@code includeScope=runtime} and {@code includeTypes=jar} would produce.
 */
public class InProcessDependencyCopier {

    // Maven's includeScope=runtime (as used by dependency:copy-dependencies) resolves to the
    // "compile" and "runtime" scopes, not literally only dependencies declared with scope=runtime.
    private static final Set<String> RUNTIME_CLASSPATH_SCOPES = Set.of("compile", "runtime");
    private static final String JAR_EXTENSION = "jar";

    private final MavenSession session;
    private final ProjectBuilder projectBuilder;

    public InProcessDependencyCopier(MavenSession session, ProjectBuilder projectBuilder) {
        this.session = session;
        this.projectBuilder = projectBuilder;
    }

    /**
     * Resolve the runtime-classpath jar dependencies of the given pom and copy them to outputDirectory,
     * using the exact file name of the resolved artifact (never reconstructed manually), so that the
     * result matches what {@code dependency:copy-dependencies} would have produced.
     *
     * @param pomFile the (generated) pom file to resolve dependencies for
     * @param outputDirectory the directory to copy resolved jars into
     * @param activeProfiles the profiles to activate while resolving the pom
     * @throws MojoExecutionException if dependency resolution or copy fails
     */
    public void copyRuntimeDependencies(File pomFile, File outputDirectory, List<String> activeProfiles)
            throws MojoExecutionException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProcessPlugins(false);
        buildingRequest.setResolveDependencies(true);
        // merge with the profiles already active on the session (e.g. from -P or settings.xml) instead of
        // overwriting them, matching MavenSessionExecutor's union semantics for the fork-based path.
        Set<String> mergedActiveProfiles = new LinkedHashSet<>(buildingRequest.getActiveProfileIds());
        mergedActiveProfiles.addAll(activeProfiles);
        buildingRequest.setActiveProfileIds(new ArrayList<>(mergedActiveProfiles));
        try {
            ProjectBuildingResult buildingResult = projectBuilder.build(pomFile, buildingRequest);
            Files.createDirectories(outputDirectory.toPath());
            DependencyResolutionResult dependencyResolutionResult = buildingResult.getDependencyResolutionResult();
            for (Dependency dependency : dependencyResolutionResult.getResolvedDependencies()) {
                if (isRuntimeClasspathJar(dependency)) {
                    copyToOutputDirectory(dependency, outputDirectory);
                }
            }
        } catch (ProjectBuildingException | IOException e) {
            throw new MojoExecutionException("Failed to resolve dependencies of " + pomFile, e);
        }
    }

    private boolean isRuntimeClasspathJar(Dependency dependency) {
        return dependency.getScope() != null && RUNTIME_CLASSPATH_SCOPES.contains(dependency.getScope())
                && JAR_EXTENSION.equals(dependency.getArtifact().getExtension())
                && dependency.getArtifact().getFile() != null;
    }

    private void copyToOutputDirectory(Dependency dependency, File outputDirectory) throws IOException {
        File resolvedFile = dependency.getArtifact().getFile();
        // reuse the resolved file name verbatim: reconstructing it manually risks a mismatch for
        // SNAPSHOT/classified artifacts, which would then be silently dropped downstream.
        File target = new File(outputDirectory, resolvedFile.getName());
        Files.copy(resolvedFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

}
