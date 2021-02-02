package org.bonitasoft.plugin.analyze.report.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DependencyReportTest {

	@Test
	void should_be_serialzed() throws IOException {
		// Given
		final ObjectMapper mapper = new ObjectMapper()
				.findAndRegisterModules()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		final DependencyReport sourceResult = new DependencyReport();
		sourceResult.getConnectorDefinitions().add(Definition.create("anId", "aVersion", "anEntryPath", "/a/file.jar"));
		sourceResult.getConnectorImplementations().add(
				Implementation.create("aClassName", "aDefId", "aDefVersion", "anImplId", "anImplVersion", "aPath", "/a/file.jar", "aSuperType")
		);
		sourceResult.getRestApiExtensions().add(RestAPIExtension.create("aName", "aDisplayName", "aDesc", "/a/file.jar"));
		sourceResult.getForms().add(Form.create("aName", "aDisplayName", "aDesc", "/a/file.jar"));
		sourceResult.getPages().add(Page.create("aName", "aDisplayName", "aDesc", "/a/file.jar"));
		sourceResult.getThemes().add(Theme.create("aName", "aDisplayName", "aDesc", "/a/file.jar"));

		// When
		final String json = mapper.writeValueAsString(sourceResult);
		final DependencyReport result = mapper.readValue(json, DependencyReport.class);

		// Then
		final DependencyReport expected = mapper.readValue(getClass().getResourceAsStream("/analysis-result.json"), DependencyReport.class);
		assertThat(result).isEqualTo(sourceResult);
		assertThat(result).isEqualTo(expected);
	}
}
