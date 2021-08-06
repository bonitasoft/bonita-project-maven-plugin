package org.bonitasoft.plugin.analyze;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

    @BeforeEach
    void setUp() throws MojoExecutionException {
        mojo = spy(new AnalyzeBonitaDependencyMojo(artifactResolver, artifactAnalyser, dependencyValidator));
        mojo.project = project;
        mojo.setLog(mock(Log.class));
    }

    @Test
    void sould_run_analysis_without_validation() throws MojoFailureException, MojoExecutionException {
        // Given
        mojo = spy(mojo);

        when(mojo.getReporters()).thenReturn(singletonList(reporter));

        List<Artifact> resolvedArtifacts = new ArrayList<>();

        final DefaultArtifact artifact = new DefaultArtifact("g", "a", "v", "runtime", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifact.setFile(new File(artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType()));
        resolvedArtifacts.add(artifact);

        when(project.getDependencyArtifacts()).thenReturn(new HashSet<>());
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
    void sould_run_analysis_with_dep_validation() throws MojoFailureException, MojoExecutionException {
        // Given
        mojo = spy(mojo);
        
        mojo.validateDeps = true ;

        when(mojo.getReporters()).thenReturn(singletonList(reporter));
        when(dependencyValidator.validate(project, buildingRequest)).thenReturn(List.of());

        List<Artifact> resolvedArtifacts = new ArrayList<>();

        final DefaultArtifact artifact = new DefaultArtifact("g", "a", "v", "runtime", "jar", null,
                new DefaultArtifactHandler("jar"));
        artifact.setFile(new File(artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType()));
        resolvedArtifacts.add(artifact);

        when(project.getDependencyArtifacts()).thenReturn(new HashSet<>());
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
