package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class AnalysisResultTest {

	@Test
	void should_be_serialzed_as_xml() throws JsonProcessingException {
		// Given
		final XmlMapper mapper = new XmlMapper();

		final AnalysisResult result = new AnalysisResult();
		result.addConnectorDefinition(new Definition("anId", "aVersion", "anEntryPath", "/a/file.jar"));
		result.addConnectorImplementation(new Implementation("aClassName", "anImplId", "anImplVersion", "aDefId", "aDefVersion", "aPath", "/a/file.jar"));
		result.addRestAPIExtension(new RestAPIExtension("aName", "aDisplayName", "aDesc", "/a/file.jar"));
		result.addForm(new Form("aName", "aDisplayName", "aDesc", "/a/file.jar"));
		result.addPage(new Page("aName", "aDisplayName", "aDesc", "/a/file.jar"));
		result.addTheme(new Theme("aName", "aDisplayName", "aDesc", "/a/file.jar"));

		// When
		final String xml = mapper.writeValueAsString(result);

		// Then
		assertThat(xml).isEqualTo("<AnalysisResult><connectorImplmentations><connectorImplmentations><className>aClassName</className><implementationId>anImplId</implementationId><implementationVersion>anImplVersion</implementationVersion><definitionId>aDefId</definitionId><definitionVersion>aDefVersion</definitionVersion><path>aPath</path><filePath>/a/file.jar</filePath><superType/></connectorImplmentations></connectorImplmentations><filterImplmentations/><connectorDefinitions><connectorDefinitions><definitionId>anId</definitionId><definitionVersion>aVersion</definitionVersion><filePath>/a/file.jar</filePath><entryPath>anEntryPath</entryPath></connectorDefinitions></connectorDefinitions><filterDefinitions/><restApiExtensions><restApiExtensions><name>aName</name><displayName>aDisplayName</displayName><description>aDesc</description><filePath>/a/file.jar</filePath></restApiExtensions></restApiExtensions><pages><pages><name>aName</name><displayName>aDisplayName</displayName><description>aDesc</description><filePath>/a/file.jar</filePath></pages></pages><forms><forms><name>aName</name><displayName>aDisplayName</displayName><description>aDesc</description><filePath>/a/file.jar</filePath></forms></forms><themes><themes><name>aName</name><displayName>aDisplayName</displayName><description>aDesc</description><filePath>/a/file.jar</filePath></themes></themes></AnalysisResult>");
	}
}
