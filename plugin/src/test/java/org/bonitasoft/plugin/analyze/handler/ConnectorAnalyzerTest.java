/** 
 * Copyright (C) 2023 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze.handler;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.connector.ConnectorResolverImpl;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader;
import org.bonitasoft.plugin.analyze.content.JarArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorAnalyzerTest {

    @Spy
    ArtifactContentReader reader = new JarArtifactContentReader();

    @InjectMocks
    ConnectorAnalyzer analyzer;

    @Mock
    ConnectorResolverImpl connectorResolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    LocalRepositoryManager localRepositoryManager;

    @Test
    void should_analyse_produce_a_report() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

        // When
        var dependencyReport = analyzer.analyze(artifact, new DependencyReport());

        // Then
        assertThat(dependencyReport).isNotNull();
    }

    @Test
    void should_report_issue_for_def_without_impl() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));
        lenient().when(connectorResolver.findAllDefinitions(eq(artifact), any(), any(Issue.Collector.class)))
                .thenReturn(List.of(Definition.create(new DescriptorIdentifier("email", "1.3.0"),
                        org.bonitasoft.plugin.analyze.report.model.Artifact.create("org.bonita.connector",
                                "bonita-connector-email", "1.3.0", null, artifact.getFile().getAbsolutePath()),
                        "email.def")));

        // When
        var dependencyReport = analyzer.analyze(artifact, new DependencyReport());

        // Then
        assertThat(dependencyReport).isNotNull();
        assertThat(dependencyReport.getIssues())
                .contains(Issue.create(Type.UNKNOWN_DEFINITION_TYPE,
                        "email.def declares a definition 'email (1.3.0)' but no matching implementation has been found. This definition will be ignored.",
                        Severity.WARNING, artifact.getId()));
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
        final boolean match = analyzer.hasMatchingImplementation(def, singletonList(impl));

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
        final boolean match = analyzer.hasMatchingImplementation(def, singletonList(impl));

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
        final boolean match = analyzer.hasMatchingImplementation(def, singletonList(impl));

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
        final boolean match = analyzer.hasMatchingImplementation(def, singletonList(impl));

        // Then
        assertThat(match).isFalse();
    }

    @Test
    void appliesToJarFile() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isTrue();
    }

    @Test
    void doesNotAppliesToZipType() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "zip",
                null, new DefaultArtifactHandler("zip"));
        artifact.setFile(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isFalse();
    }

    @Test
    void doesNotAppliesToInvalidFile() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(new File("doesNotExists"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isFalse();
    }

    @Test
    void doesNotAppliesWhenDetectionFails() throws Exception {
        try {
            // Given
            doThrow(IOException.class).when(reader).readFirstEntry(any(), any(), any());
            var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime",
                    "jar",
                    null, new DefaultArtifactHandler("jar"));
            artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

            // Then
            assertThat(analyzer.appliesTo(artifact)).isFalse();
        } finally {
            // Reset the reader spy to avoid side effects on other tests
            lenient().doCallRealMethod().when(reader).readFirstEntry(any(), any(), any());
        }
    }
}
