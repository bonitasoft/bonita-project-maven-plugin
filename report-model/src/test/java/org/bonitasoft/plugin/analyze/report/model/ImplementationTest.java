package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ImplementationTest {

	@Test
	void should_be_serialzed_as_xml() throws JsonProcessingException {
		// Given
		final XmlMapper mapper = new XmlMapper();
		final Implementation elem = new Implementation("aClassName", "anImplId", "anImplVersion", "aDefId","aDefVersion","aPath","/a/file.jar");

		// When
		final String xml = mapper.writeValueAsString(elem);

		// Then
		assertThat(xml).isEqualTo("<Implementation><className>aClassName</className><implementationId>anImplId</implementationId><implementationVersion>anImplVersion</implementationVersion><definitionId>aDefId</definitionId><definitionVersion>aDefVersion</definitionVersion><path>aPath</path><filePath>/a/file.jar</filePath><superType/></Implementation>");
	}
}
