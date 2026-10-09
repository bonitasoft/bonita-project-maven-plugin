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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InProcessDependencyCopierTest {

    @Mock
    MavenSession session;

    @Mock
    ProjectBuilder projectBuilder;

    @Mock
    ProjectBuildingResult buildingResult;

    @Mock
    DependencyResolutionResult dependencyResolutionResult;

    @TempDir
    Path sourceRepo;

    @TempDir
    Path outputDir;

    private InProcessDependencyCopier copier;

    private void givenResolvedDependencies(List<Dependency> dependencies) throws Exception {
        when(session.getProjectBuildingRequest()).thenReturn(new DefaultProjectBuildingRequest());
        when(projectBuilder.build(any(File.class), any(ProjectBuildingRequest.class))).thenReturn(buildingResult);
        when(buildingResult.getDependencyResolutionResult()).thenReturn(dependencyResolutionResult);
        when(dependencyResolutionResult.getCollectionErrors()).thenReturn(List.of());
        when(dependencyResolutionResult.getUnresolvedDependencies()).thenReturn(List.of());
        when(dependencyResolutionResult.getResolvedDependencies()).thenReturn(dependencies);
        copier = new InProcessDependencyCopier(session, projectBuilder);
    }

    private Dependency runtimeDependency(String groupId, String artifactId, String classifier, String version,
            String scope, String extension, String content) throws Exception {
        File file = sourceRepo.resolve(artifactId + "-" + version
                + (classifier.isEmpty() ? "" : "-" + classifier) + "." + extension).toFile();
        Files.writeString(file.toPath(), content);
        var artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version, Map.of(), file);
        return new Dependency(artifact, scope);
    }

    @Test
    void should_copy_runtime_scope_jar_dependency() throws Exception {
        Dependency dependency = runtimeDependency("com.company", "my-lib", "", "1.0.0", "runtime", "jar",
                "runtime-content");
        givenResolvedDependencies(List.of(dependency));

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of());

        assertThat(outputDir.resolve("my-lib-1.0.0.jar")).exists().hasContent("runtime-content");
    }

    @Test
    void should_copy_compile_scope_jar_dependency() throws Exception {
        // includeScope=runtime (as used by dependency:copy-dependencies) also includes "compile" scope
        Dependency dependency = runtimeDependency("com.company", "my-lib", "", "1.0.0", "compile", "jar",
                "compile-content");
        givenResolvedDependencies(List.of(dependency));

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of());

        assertThat(outputDir.resolve("my-lib-1.0.0.jar")).exists().hasContent("compile-content");
    }

    @Test
    void should_copy_snapshot_dependency_with_exact_resolved_file_name() throws Exception {
        Dependency dependency = runtimeDependency("com.company", "my-lib", "", "1.0.0-SNAPSHOT", "runtime", "jar",
                "snapshot-content");
        givenResolvedDependencies(List.of(dependency));

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of());

        assertThat(outputDir.resolve("my-lib-1.0.0-SNAPSHOT.jar")).exists().hasContent("snapshot-content");
    }

    @Test
    void should_copy_classified_dependency_with_classifier_in_file_name() throws Exception {
        Dependency dependency = runtimeDependency("com.company", "connector-impl", "impl", "2.0.0", "runtime", "jar",
                "classified-content");
        givenResolvedDependencies(List.of(dependency));

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of());

        assertThat(outputDir.resolve("connector-impl-2.0.0-impl.jar")).exists().hasContent("classified-content");
    }

    @Test
    void should_not_copy_test_scope_dependency() throws Exception {
        Dependency dependency = runtimeDependency("com.company", "test-only", "", "1.0.0", "test", "jar",
                "test-content");
        givenResolvedDependencies(List.of(dependency));

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of());

        assertThat(outputDir.resolve("test-only-1.0.0.jar")).doesNotExist();
    }

    @Test
    void should_not_copy_non_jar_dependency() throws Exception {
        Dependency dependency = runtimeDependency("com.company", "some-pom", "", "1.0.0", "runtime", "pom",
                "pom-content");
        givenResolvedDependencies(List.of(dependency));

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of());

        assertThat(outputDir.resolve("some-pom-1.0.0.pom")).doesNotExist();
    }

    @Test
    void should_activate_given_profiles_and_resolve_dependencies_without_processing_plugins() throws Exception {
        givenResolvedDependencies(List.of());
        ArgumentCaptor<ProjectBuildingRequest> captor = ArgumentCaptor.forClass(ProjectBuildingRequest.class);

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of("env-Production"));

        verify(projectBuilder).build(any(File.class), captor.capture());
        ProjectBuildingRequest usedRequest = captor.getValue();
        assertThat(usedRequest.getActiveProfileIds()).containsExactly("env-Production");
        assertThat(usedRequest.isResolveDependencies()).isTrue();
        assertThat(usedRequest.isProcessPlugins()).isFalse();
    }

    @Test
    void should_merge_given_profiles_with_profiles_already_active_on_the_session() throws Exception {
        DefaultProjectBuildingRequest sessionRequest = new DefaultProjectBuildingRequest();
        sessionRequest.setActiveProfileIds(List.of("env-Base"));
        when(session.getProjectBuildingRequest()).thenReturn(sessionRequest);
        when(projectBuilder.build(any(File.class), any(ProjectBuildingRequest.class))).thenReturn(buildingResult);
        when(buildingResult.getDependencyResolutionResult()).thenReturn(dependencyResolutionResult);
        when(dependencyResolutionResult.getCollectionErrors()).thenReturn(List.of());
        when(dependencyResolutionResult.getUnresolvedDependencies()).thenReturn(List.of());
        when(dependencyResolutionResult.getResolvedDependencies()).thenReturn(List.of());
        copier = new InProcessDependencyCopier(session, projectBuilder);
        ArgumentCaptor<ProjectBuildingRequest> captor = ArgumentCaptor.forClass(ProjectBuildingRequest.class);

        copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of("env-Production"));

        verify(projectBuilder).build(any(File.class), captor.capture());
        assertThat(captor.getValue().getActiveProfileIds()).containsExactlyInAnyOrder("env-Base", "env-Production");
    }

    @Test
    void should_wrap_project_building_exception_in_mojo_execution_exception() throws Exception {
        when(session.getProjectBuildingRequest()).thenReturn(new DefaultProjectBuildingRequest());
        when(projectBuilder.build(any(File.class), any(ProjectBuildingRequest.class)))
                .thenThrow(new ProjectBuildingException("id", "failed", (File) null));
        copier = new InProcessDependencyCopier(session, projectBuilder);

        assertThatThrownBy(() -> copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of()))
                .isInstanceOf(MojoExecutionException.class)
                .hasCauseInstanceOf(ProjectBuildingException.class);
    }

    @Test
    void should_fail_when_dependency_collection_has_errors() throws Exception {
        when(session.getProjectBuildingRequest()).thenReturn(new DefaultProjectBuildingRequest());
        when(projectBuilder.build(any(File.class), any(ProjectBuildingRequest.class))).thenReturn(buildingResult);
        when(buildingResult.getDependencyResolutionResult()).thenReturn(dependencyResolutionResult);
        when(dependencyResolutionResult.getCollectionErrors())
                .thenReturn(List.of(new Exception("could not compute the dependency graph")));
        copier = new InProcessDependencyCopier(session, projectBuilder);

        assertThatThrownBy(() -> copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of()))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("could not compute the dependency graph");
    }

    @Test
    void should_fail_when_a_dependency_is_unresolved_instead_of_silently_skipping_it() throws Exception {
        Dependency unresolved = runtimeDependency("com.company", "missing-lib", "", "1.0.0", "runtime", "jar",
                "irrelevant");
        when(session.getProjectBuildingRequest()).thenReturn(new DefaultProjectBuildingRequest());
        when(projectBuilder.build(any(File.class), any(ProjectBuildingRequest.class))).thenReturn(buildingResult);
        when(buildingResult.getDependencyResolutionResult()).thenReturn(dependencyResolutionResult);
        when(dependencyResolutionResult.getCollectionErrors()).thenReturn(List.of());
        when(dependencyResolutionResult.getUnresolvedDependencies()).thenReturn(List.of(unresolved));
        when(dependencyResolutionResult.getResolutionErrors(unresolved))
                .thenReturn(List.of(new Exception("artifact not found in any repository")));
        copier = new InProcessDependencyCopier(session, projectBuilder);

        assertThatThrownBy(() -> copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of()))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("missing-lib")
                .hasMessageContaining("artifact not found in any repository");
    }

    @Test
    void should_fail_when_resolved_artifact_is_a_directory_instead_of_a_jar() throws Exception {
        // happens when a dependency resolves to an unpackaged reactor sibling module via Maven's
        // workspace reader (e.g. its target/classes directory) instead of a real jar file
        Path reactorClassesDir = sourceRepo.resolve("target/classes");
        Files.createDirectories(reactorClassesDir);
        var artifact = new DefaultArtifact("com.company", "sibling-module", "", "jar", "1.0.0", Map.of(),
                reactorClassesDir.toFile());
        Dependency dependency = new Dependency(artifact, "compile");
        givenResolvedDependencies(List.of(dependency));

        assertThatThrownBy(() -> copier.copyRuntimeDependencies(new File("pom.xml"), outputDir.toFile(), List.of()))
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("sibling-module");
        assertThat(outputDir).isEmptyDirectory();
    }

}
