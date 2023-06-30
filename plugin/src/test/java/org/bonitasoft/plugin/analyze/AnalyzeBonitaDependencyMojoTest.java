/** 
 * Copyright (C) 2023 BonitaSoft S.A.
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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.lifecycle.internal.ProjectArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.bonitasoft.plugin.analyze.report.DependencyReporter;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyzeBonitaDependencyMojoTest {

    AnalyzeBonitaDependencyMojo mojo;

    @Mock
    ArtifactResolver artifactResolver;

    @Mock
    MavenProject project;

    @Mock
    ArtifactAnalyser artifactAnalyser;

    @Mock
    DependencyGraphBuilder dependencyGraphBuilder;

    @Mock
    DependencyReporter reporter;

    @Mock
    DependencyValidator dependencyValidator;

    @Mock
    ProjectBuildingRequest buildingRequest;

    @Mock
    ProjectArtifactFactory artifactFactory;

    @Captor
    ArgumentCaptor<List<Artifact>> resolvedArtifacts;

    @BeforeEach
    void setUp() throws MojoExecutionException {
        mojo = spy(new AnalyzeBonitaDependencyMojo(artifactResolver, artifactAnalyser, dependencyValidator,
                artifactFactory));
        mojo.project = project;
        mojo.setLog(mock(Log.class));
    }

    @Test
    void should_run_analysis_without_validation() throws Exception {
        // Given
        mojo = spy(mojo);

        when(mojo.getReporters()).thenReturn(singletonList(reporter));

        List<Artifact> resolvedArtifacts = new ArrayList<>();

        final DefaultArtifact artifact = new DefaultArtifact("g", "a", "v", "runtime", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifact.setFile(new File(artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType()));
        resolvedArtifacts.add(artifact);

        when(artifactFactory.createArtifacts(project)).thenReturn(new HashSet<>());
        doReturn(buildingRequest).when(mojo).newProjectBuildingRequest();
        doReturn(resolvedArtifacts).when(mojo).resolveArtifacts(any(), Mockito.eq(buildingRequest));
        when(artifactAnalyser.analyse(any())).thenReturn(new DependencyReport());

        // When
        mojo.execute();

        // Then
        verify(artifactAnalyser).analyse(resolvedArtifacts);
        verify(reporter).report(any());
    }

    @Test
    void shouldIncludeOnlyRuntimeScopeByDefault() throws Exception {
        // Given
        mojo = spy(mojo);

        when(mojo.getReporters()).thenReturn(singletonList(reporter));

        var artifactWithRuntimeScope = new DefaultArtifact("g", "a", "v", "compile", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifactWithRuntimeScope.setFile(new File(artifactWithRuntimeScope.getArtifactId() + "-"
                + artifactWithRuntimeScope.getVersion() + "." + artifactWithRuntimeScope.getType()));

        var artifactWithProvidedScope = new DefaultArtifact("g", "b", "v", "provided", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifactWithProvidedScope.setFile(new File(artifactWithProvidedScope.getArtifactId() + "-"
                + artifactWithProvidedScope.getVersion() + "." + artifactWithProvidedScope.getType()));

        when(artifactFactory.createArtifacts(project))
                .thenReturn(Set.of(artifactWithProvidedScope, artifactWithRuntimeScope));
        doReturn(buildingRequest).when(mojo).newProjectBuildingRequest();
        doReturn(artifactWithRuntimeScope).when(mojo).resolve(buildingRequest, artifactWithRuntimeScope);
        when(artifactAnalyser.analyse(any())).thenReturn(new DependencyReport());

        // When
        mojo.execute();

        // Then
        verify(artifactAnalyser).analyse(resolvedArtifacts.capture());
        assertThat(resolvedArtifacts.getValue())
                .hasSize(1)
                .extracting("scope").containsOnly("compile");
        verify(mojo, never()).resolve(buildingRequest, artifactWithProvidedScope);
        verify(reporter).report(any());
    }

    @Test
    void shouldIncludeCompileScope() throws Exception {
        // Given
        mojo = spy(mojo);
        mojo.includeScope = "compile";

        when(mojo.getReporters()).thenReturn(singletonList(reporter));

        var artifactWithRuntimeScope = new DefaultArtifact("g", "a", "v", "compile", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifactWithRuntimeScope.setFile(new File(artifactWithRuntimeScope.getArtifactId() + "-"
                + artifactWithRuntimeScope.getVersion() + "." + artifactWithRuntimeScope.getType()));

        var artifactWithProvidedScope = new DefaultArtifact("g", "b", "v", "provided", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifactWithProvidedScope.setFile(new File(artifactWithProvidedScope.getArtifactId() + "-"
                + artifactWithProvidedScope.getVersion() + "." + artifactWithProvidedScope.getType()));

        when(artifactFactory.createArtifacts(project))
                .thenReturn(Set.of(artifactWithProvidedScope, artifactWithRuntimeScope));
        doReturn(buildingRequest).when(mojo).newProjectBuildingRequest();
        doReturn(artifactWithRuntimeScope).when(mojo).resolve(buildingRequest, artifactWithRuntimeScope);
        doReturn(artifactWithProvidedScope).when(mojo).resolve(buildingRequest, artifactWithProvidedScope);
        when(artifactAnalyser.analyse(any())).thenReturn(new DependencyReport());

        // When
        mojo.execute();

        // Then
        verify(artifactAnalyser).analyse(resolvedArtifacts.capture());
        assertThat(resolvedArtifacts.getValue())
                .hasSize(2)
                .extracting("scope").contains("compile", "provided");
        verify(reporter).report(any());
    }

    @Test
    void sould_run_analysis_with_dep_validation() throws Exception {
        // Given
        mojo = spy(mojo);

        mojo.validateDeps = true;

        when(mojo.getReporters()).thenReturn(singletonList(reporter));
        when(dependencyValidator.validate(project, buildingRequest)).thenReturn(List.of());

        List<Artifact> resolvedArtifacts = new ArrayList<>();

        final DefaultArtifact artifact = new DefaultArtifact("g", "a", "v", "runtime", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifact.setFile(new File(artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType()));
        resolvedArtifacts.add(artifact);

        when(artifactFactory.createArtifacts(project)).thenReturn(new HashSet<>());
        doReturn(buildingRequest).when(mojo).newProjectBuildingRequest();
        doReturn(resolvedArtifacts).when(mojo).resolveArtifacts(any(), Mockito.eq(buildingRequest));
        when(artifactAnalyser.analyse(any())).thenReturn(new DependencyReport());

        // When
        mojo.execute();

        // Then
        verify(artifactAnalyser).analyse(resolvedArtifacts);
        verify(dependencyValidator).validate(project, buildingRequest);
        verify(reporter).report(any());
    }
}
