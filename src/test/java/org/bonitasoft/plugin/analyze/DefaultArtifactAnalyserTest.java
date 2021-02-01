package org.bonitasoft.plugin.analyze;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Definition;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Implementation;
import org.bonitasoft.plugin.analyze.DefaultArtifactAnalyser.CustomPageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
		artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

		List<Artifact> artifacts = singletonList(artifact);

		// When
		final AnalysisResult analysisResult = analyser.analyse(artifacts);

		// Then
		assertThat(analysisResult).isNotNull();
	}

	@Test
	void impl_should_match() {
		// Given
		final String defId = "id-a";
		final String defVersion = "1.0.0";

		Definition def = mock(Definition.class);
		lenient().when(def.getDefinitionId()).thenReturn(defId);
		lenient().when(def.getDefinitionVersion()).thenReturn(defVersion);

		final Implementation impl = mock(Implementation.class);
		lenient().when(impl.getDefinitionId()).thenReturn(defId);
		lenient().when(impl.getDefinitionVersion()).thenReturn(defVersion);

		// When
		final boolean match = analyser.hasMatchingImplementation(def, singletonList(impl));

		// Then
		assertThat(match).isTrue();
	}

	@Test
	void impl_should_not_match() {
		// Given
		final String defId = "id-a";
		final String defVersion = "1.0.0";
		Definition def = mock(Definition.class);
		lenient().when(def.getDefinitionId()).thenReturn(defId);
		lenient().when(def.getDefinitionVersion()).thenReturn(defVersion);

		final Implementation impl = mock(Implementation.class);
		lenient().when(impl.getDefinitionId()).thenReturn("anotherId");
		lenient().when(impl.getDefinitionVersion()).thenReturn("anotherVersion");

		// When
		final boolean match = analyser.hasMatchingImplementation(def, singletonList(impl));

		// Then
		assertThat(match).isFalse();
	}

	@Test
	void impl_should_not_match_on_id() {
		// Given
		final String defId = "id-a";
		final String defVersion = "1.0.0";

		Definition def = mock(Definition.class);
		lenient().when(def.getDefinitionId()).thenReturn(defId);
		lenient().when(def.getDefinitionVersion()).thenReturn(defVersion);

		final Implementation impl = mock(Implementation.class);
		lenient().when(impl.getDefinitionId()).thenReturn("anotherId");
		lenient().when(impl.getDefinitionVersion()).thenReturn(defVersion);

		// When
		final boolean match = analyser.hasMatchingImplementation(def, singletonList(impl));

		// Then
		assertThat(match).isFalse();
	}

	@Test
	void impl_should_not_match_on_version() {
		// Given
		final String defId = "id-a";
		final String defVersion = "1.0.0";

		Definition def = mock(Definition.class);
		lenient().when(def.getDefinitionId()).thenReturn(defId);
		lenient().when(def.getDefinitionVersion()).thenReturn(defVersion);

		final Implementation impl = mock(Implementation.class);
		lenient().when(impl.getDefinitionId()).thenReturn(defId);
		lenient().when(impl.getDefinitionVersion()).thenReturn("anotherVersion");

		// When
		final boolean match = analyser.hasMatchingImplementation(def, singletonList(impl));

		// Then
		assertThat(match).isFalse();
	}

	@ParameterizedTest
	@EnumSource(CustomPageType.class)
	void should_detect_custom_page_type(CustomPageType customPageType) throws IOException {
		// Given
		DefaultArtifactAnalyser spy = spy(analyser);
		Properties properties = new Properties();
		properties.setProperty("contentType", customPageType.getValue());
		doReturn(properties).when(spy).readPageProperties(any());
		final Artifact artifact = mock(Artifact.class);
		final AnalysisResult analysisResult = mock(AnalysisResult.class);

		// When
		spy.analyseCustomPageArtifact(artifact, analysisResult);

		// Then
		switch (customPageType) {
			case PAGE:
				verify(analysisResult).addPage(any());
				break;
			case FORM:
				verify(analysisResult).addForm(any());
				break;
			case THEME:
				verify(analysisResult).addTheme(any());
				break;
			case API_EXTENSION:
				verify(analysisResult).addRestAPIExtension(any());
				break;
			default:
				fail("Custom page type no supported:" + customPageType);
		}
	}

	@Test
	void should_read_customPage_properties() throws Exception {
		// Given
		File file = getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip");

		Properties expectedProperties = new Properties();
		expectedProperties.load(getClass().getResourceAsStream("/page.properties"));

		// When
		final Properties properties = analyser.readPageProperties(file);

		// Then
		assertThat(properties).isEqualTo(expectedProperties);
	}

	@Test
	void should_have_customPageDescriptor() throws Exception {
		// Given
		File file = getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip");
		// When
		final boolean found = analyser.hasCustomPageDescriptor(file);
		// Then
		assertThat(found).isTrue();
	}

	@Test
	void should_not_have_customPageDescriptor() throws Exception {
		// Given
		File file = getResourceAsFile("/bonita-actorfilter-single-user-1.0.0.jar");
		// When
		final boolean found = analyser.hasCustomPageDescriptor(file);
		// Then
		assertThat(found).isFalse();
	}

	@Test
	void should_have_connectorDescriptor() throws Exception {
		// Given
		File file = getResourceAsFile("/bonita-connector-email-1.3.0.jar");
		// When
		final boolean found = analyser.hasConnectorDescriptor(file);
		// Then
		assertThat(found).isTrue();
	}

	@Test
	void should_not_have_connectorDescriptor() throws Exception {
		// Given
		File file = getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip");
		// When
		final boolean found = analyser.hasConnectorDescriptor(file);
		// Then
		assertThat(found).isFalse();
	}
}
