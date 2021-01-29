package org.bonitasoft.plugin.analyze;

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
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	AnalysisResultReporter reporter;

	@BeforeEach
	void setUp() {
		mojo = spy(new AnalyzeBonitaDependencyMojo(artifactResolver, artifactAnalyser));
		mojo.project = project;
		mojo.setLog(mock(Log.class));
		mojo.reporters = singletonList(reporter);
	}

	@Test
	void sould_run_analysis() throws MojoFailureException, MojoExecutionException {
		// Given
		mojo = spy(mojo);

		List<Artifact> resolvedArtifacts = new ArrayList<>();

		final DefaultArtifact artifact = new DefaultArtifact("g", "a", "v", "runtime", "jar", null, new DefaultArtifactHandler("jar"));
		artifact.setFile(new File(artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType()));
		resolvedArtifacts.add(artifact);

		when(project.getDependencyArtifacts()).thenReturn(new HashSet<>());
		doReturn(resolvedArtifacts).when(mojo).resolveArtifacts(any());
		when(artifactAnalyser.analyse(any())).thenReturn(new AnalysisResult());

		// When
		mojo.execute();

		// Then
		verify(artifactAnalyser).analyse(resolvedArtifacts);
		verify(reporter).report(any());
	}
}
