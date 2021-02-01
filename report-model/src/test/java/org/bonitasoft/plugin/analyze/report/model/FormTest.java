package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FormTest {

	@Test
	void should_be_serialzed_as_xml() throws JsonProcessingException {
		// Given
		final XmlMapper mapper = new XmlMapper();
		final Form elem = new Form("aName", "aDisplayName", "aDesc", "/a/file.jar");

		// When
		final String xml = mapper.writeValueAsString(elem);

		// Then
		assertThat(xml).isEqualTo("<Form><name>aName</name><displayName>aDisplayName</displayName><description>aDesc</description><filePath>/a/file.jar</filePath></Form>");
	}
}
