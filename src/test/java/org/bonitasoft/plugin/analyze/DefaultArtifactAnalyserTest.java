package org.bonitasoft.plugin.analyze;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefaultArtifactAnalyserTest {

	@InjectMocks
	DefaultArtifactAnalyser analyser;

	@Mock
	ConnectorResolver connectorResolver;

	@Test
	void should_analyse() throws URISyntaxException {
		// Given

		final DefaultArtifact artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar", null, new DefaultArtifactHandler("jar"));
		artifact.setFile(Paths.get(getClass().getResource("/bonita-connector-email-1.3.0.jar").toURI()).toFile());

		List<Artifact> artifacts = Collections.singletonList(artifact);

		// When
		final AnalysisResult analysisResult = analyser.analyse(artifacts);

		// Then
		assertThat(analysisResult).isNotNull();
	}
}
