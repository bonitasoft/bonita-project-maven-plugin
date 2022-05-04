package org.bonitasoft.plugin.analyze;

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
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.cfr.CFRConnectorResolver;
import org.bonitasoft.plugin.analyze.report.model.CustomPage.CustomPageType;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultArtifactAnalyserTest {

    @InjectMocks
    DefaultArtifactAnalyser analyser;

    @Mock
    CFRConnectorResolver connectorResolver;

    @Mock
    IssueCollector issueCollector;

    @BeforeEach
    void setup() throws Exception {
        lenient().when(issueCollector.getIssues()).thenReturn(List.of());
    }

    @Test
    void should_analyse_produce_a_report() throws URISyntaxException {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

        // When
        var dependencyReport = analyser.analyse(List.of(artifact));

        // Then
        assertThat(dependencyReport).isNotNull();
    }

    @Test
    void should_report_issue_for_def_without_impl() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));
        lenient().when(connectorResolver.findAllDefinitions(artifact, issueCollector))
                .thenReturn(List.of(Definition.create(new DescriptorIdentifier("email", "1.3.0"),
                        org.bonitasoft.plugin.analyze.report.model.Artifact.create("org.bonita.connector",
                                "bonita-connector-email", "1.3.0", null, artifact.getFile().getAbsolutePath()),
                        "email.def")));

        // When
        var dependencyReport = analyser.analyse(List.of(artifact));

        // Then
        assertThat(dependencyReport).isNotNull();
        assertThat(dependencyReport.getIssues())
            .contains(Issue.create(Type.UNKNOWN_DEFINITION_TYPE, "email.def declares a definition 'email (1.3.0)' but no matching implementation has been found. This definition will be ignored.", Severity.WARNING, artifact.getId()));
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
    @ValueSource(strings = { "page", "form", "theme", "apiExtension" })
    void should_detect_custom_page_type(String customPageType) throws IOException {
        // Given
        DefaultArtifactAnalyser spy = spy(analyser);
        Properties properties = new Properties();
        properties.setProperty("contentType", customPageType);
        doReturn(properties).when(spy).readPageProperties(any());
        final Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(new File("/somewhere-over-the-rain.bow"));
        final DependencyReport dependencyReport = mock(DependencyReport.class);

        // When
        spy.analyseCustomPageArtifact(artifact, dependencyReport);

        // Then
        switch (CustomPageType.valueOf(customPageType.toUpperCase())) {
            case PAGE:
                verify(dependencyReport).addPage(any());
                break;
            case FORM:
                verify(dependencyReport).addForm(any());
                break;
            case THEME:
                verify(dependencyReport).addTheme(any());
                break;
            case APIEXTENSION:
                verify(dependencyReport).addRestAPIExtension(any());
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
