package org.bonitasoft.plugin.analyze.report.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestAPIExtensionTest {

	@Test
	void should_be_serialzed_as_xml() throws JsonProcessingException {
		// Given
		final XmlMapper mapper = new XmlMapper();
		final RestAPIExtension restAPIExtension = new RestAPIExtension("aName", "aDisplayName", "aDesc", "/a/file.jar");

		// When
		final String xml = mapper.writeValueAsString(restAPIExtension);

		// Then
		assertThat(xml).isEqualTo("<RestAPIExtension><name>aName</name><displayName>aDisplayName</displayName><description>aDesc</description><filePath>/a/file.jar</filePath></RestAPIExtension>");
	}
}
