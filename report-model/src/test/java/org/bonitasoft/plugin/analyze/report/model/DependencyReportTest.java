package org.bonitasoft.plugin.analyze.report.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
        Artifact artifact = Artifact.create("group.id", "artifact.id", "1.0.0", null, "/a/file.jar");

        final DependencyReport sourceResult = new DependencyReport();
        sourceResult.getConnectorDefinitions()
                .add(Definition.create(new DescriptorIdentifier("anId", "aVersion"), artifact, "connector.def"));
        sourceResult.getConnectorImplementations().add(
                ConnectorImplementation.create("aClassName",
                        new DescriptorIdentifier("aDefId", "aDefVersion"),
                        new DescriptorIdentifier("anImplId", "anImplVersion"),
                        artifact,
                        "connector.impl"));
        sourceResult.getFilterDefinitions()
                .add(Definition.create(new DescriptorIdentifier("anId", "aVersion"), artifact, "filter.def"));
        sourceResult.getFilterImplementations().add(ActorFilterImplementation.create("aClassName",
                new DescriptorIdentifier("aDefId", "aDefVersion"),
                new DescriptorIdentifier("anImplId", "anImplVersion"),
                artifact,
                "filter.impl"));

        sourceResult.getRestApiExtensions().add(RestAPIExtension.create("aName", "aDisplayName", "aDesc", artifact));
        sourceResult.getForms().add(Form.create("aName", "aDisplayName", "aDesc", artifact));
        sourceResult.getPages().add(Page.create("aName", "aDisplayName", "aDesc", artifact));
        sourceResult.getThemes().add(Theme.create("aName", "aDisplayName", "aDesc", artifact));

        // When
        final String json = mapper.writeValueAsString(sourceResult);
        final DependencyReport result = mapper.readValue(json, DependencyReport.class);

        // Then
        final DependencyReport expected = mapper.readValue(getClass().getResourceAsStream("/analysis-result.json"),
                DependencyReport.class);
        assertThat(result)
                .isEqualTo(sourceResult)
                .isEqualTo(expected);
    }
}
