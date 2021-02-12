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

        final DependencyReport sourceResult = new DependencyReport();
        MavenArtifact mavenArtifact = MavenArtifact.create("groupId", "artifactID", "1.0.0", "classifier", "type");
        sourceResult.getConnectorDefinitions().add(Definition.create(new DescriptorIdentifier("anId", "aVersion"),
                "/a/file.jar", "connector.def", mavenArtifact));
        sourceResult.getConnectorImplementations().add(
                ConnectorImplementation.create("aClassName",
                        new DescriptorIdentifier("aDefId", "aDefVersion"),
                        new DescriptorIdentifier("anImplId", "anImplVersion"),
                        "/a/file.jar",
                        "connector.impl",
                        mavenArtifact));
        sourceResult.getFilterDefinitions().add(Definition.create(new DescriptorIdentifier("anId", "aVersion"),
                "/a/file.jar", "filter.def", mavenArtifact));
        sourceResult.getFilterImplementations().add(ActorFilterImplementation.create("aClassName",
                new DescriptorIdentifier("aDefId", "aDefVersion"),
                new DescriptorIdentifier("anImplId", "anImplVersion"),
                "/a/file.jar",
                "filter.impl",
                mavenArtifact));
        sourceResult.getRestApiExtensions()
                .add(RestAPIExtension.create("aName", "aDisplayName", "aDesc", "/a/file.jar", mavenArtifact));
        sourceResult.getForms().add(Form.create("aName", "aDisplayName", "aDesc", "/a/file.jar", mavenArtifact));
        sourceResult.getPages().add(Page.create("aName", "aDisplayName", "aDesc", "/a/file.jar", mavenArtifact));
        sourceResult.getThemes().add(Theme.create("aName", "aDisplayName", "aDesc", "/a/file.jar", mavenArtifact));

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
