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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.plugin.MojoExecutionException;
import org.bonitasoft.bonita2bar.BuildBarException;
import org.bonitasoft.bonita2bar.MavenExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarMavenExecutorTest {

    @Mock
    InProcessDependencyCopier copier;

    @Mock
    MavenExecutor fallback;

    @TempDir
    Path pomDirectory;

    private final Supplier<String> errorMessageBase = () -> "error";

    // the real call made by bonita2bar's DependenciesArtifactProvider: a goal-matching outputDirectory
    // (relative to the pom's directory) plus the literal includeScope=runtime/includeTypes=jar properties
    private Map<String, String> copyDependenciesProperties(String outputDirectory) {
        return Map.of(
                "outputDirectory", outputDirectory,
                "includeScope", "runtime",
                "includeTypes", "jar");
    }

    @Test
    void should_dispatch_copy_dependencies_call_to_in_process_copier() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = pomDirectory.resolve("pom.xml").toFile();
        Map<String, String> properties = copyDependenciesProperties("./dependencies");

        executor.execute(pomFile, List.of("dependency:copy-dependencies"), properties, List.of("env-Production"),
                errorMessageBase);

        ArgumentCaptor<File> outputDirectoryCaptor = ArgumentCaptor.forClass(File.class);
        verify(copier).copyRuntimeDependencies(eq(pomFile), outputDirectoryCaptor.capture(),
                eq(List.of("env-Production")));
        assertThat(outputDirectoryCaptor.getValue().getCanonicalFile())
                .isEqualTo(pomDirectory.resolve("dependencies").toFile().getCanonicalFile());
        verifyNoInteractions(fallback);
    }

    @Test
    void should_resolve_absolute_output_directory_as_is() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = pomDirectory.resolve("pom.xml").toFile();
        File absoluteOutputDirectory = pomDirectory.resolve("elsewhere/dependencies").toFile();
        Map<String, String> properties = copyDependenciesProperties(absoluteOutputDirectory.getAbsolutePath());

        executor.execute(pomFile, List.of("dependency:copy-dependencies"), properties, List.of(), errorMessageBase);

        verify(copier).copyRuntimeDependencies(eq(pomFile), eq(absoluteOutputDirectory), eq(List.of()));
    }

    @Test
    void should_fallback_for_any_other_goal() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = new File("pom.xml");
        Map<String, String> properties = Map.of();

        executor.execute(pomFile, List.of("clean"), properties, List.of(), errorMessageBase);

        verify(fallback).execute(pomFile, List.of("clean"), properties, List.of(), errorMessageBase);
        verifyNoInteractions(copier);
    }

    @Test
    void should_fallback_when_copy_dependencies_goal_is_combined_with_another_goal() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = new File("pom.xml");
        Map<String, String> properties = copyDependenciesProperties("./dependencies");
        List<String> goals = List.of("dependency:copy-dependencies", "clean");

        executor.execute(pomFile, goals, properties, List.of(), errorMessageBase);

        verify(fallback).execute(pomFile, goals, properties, List.of(), errorMessageBase);
        verifyNoInteractions(copier);
    }

    @Test
    void should_fallback_when_output_directory_property_is_missing() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = new File("pom.xml");
        List<String> goals = List.of("dependency:copy-dependencies");
        Map<String, String> properties = Map.of("includeScope", "runtime", "includeTypes", "jar");

        executor.execute(pomFile, goals, properties, List.of(), errorMessageBase);

        verify(fallback).execute(pomFile, goals, properties, List.of(), errorMessageBase);
        verifyNoInteractions(copier);
    }

    @Test
    void should_fallback_when_include_scope_or_include_types_do_not_match() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = new File("pom.xml");
        List<String> goals = List.of("dependency:copy-dependencies");
        Map<String, String> properties = Map.of(
                "outputDirectory", "./dependencies",
                "includeScope", "compile",
                "includeTypes", "jar");

        executor.execute(pomFile, goals, properties, List.of(), errorMessageBase);

        verify(fallback).execute(pomFile, goals, properties, List.of(), errorMessageBase);
        verifyNoInteractions(copier);
    }

    @Test
    void should_wrap_copier_failure_into_build_bar_exception_without_calling_fallback() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = pomDirectory.resolve("pom.xml").toFile();
        Map<String, String> properties = copyDependenciesProperties("./dependencies");
        doThrow(new MojoExecutionException("resolution failed")).when(copier)
                .copyRuntimeDependencies(any(File.class), any(File.class), any());

        assertThatThrownBy(() -> executor.execute(pomFile, List.of("dependency:copy-dependencies"), properties,
                List.of(), errorMessageBase))
                .isInstanceOf(BuildBarException.class)
                .hasCauseInstanceOf(MojoExecutionException.class);
        verify(fallback, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void should_propagate_fallback_build_bar_exception() throws Exception {
        var executor = new BarMavenExecutor(copier, fallback);
        File pomFile = new File("pom.xml");
        doThrow(new BuildBarException("fallback failed")).when(fallback).execute(any(), any(), any(), any(), any());

        assertThatThrownBy(
                () -> executor.execute(pomFile, List.of("clean"), Map.of(), List.of(), errorMessageBase))
                .isInstanceOf(BuildBarException.class)
                .hasMessage("fallback failed");
    }

}
