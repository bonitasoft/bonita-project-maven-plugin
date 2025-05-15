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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader;
import org.bonitasoft.plugin.analyze.content.ZipArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.model.CustomPage.CustomPageType;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomPageAnalyzerTest {

    @Spy
    ArtifactContentReader reader = new ZipArtifactContentReader();

    @InjectMocks
    CustomPageAnalyzer analyzer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    LocalRepositoryManager localRepositoryManager;

    @ParameterizedTest
    @ValueSource(strings = { "page", "form", "theme", "apiExtension" })
    void should_detect_custom_page_type(String customPageType) throws IOException {
        // Given
        var spy = spy(analyzer);
        Properties properties = new Properties();
        properties.setProperty("contentType", customPageType);
        final Artifact artifact = mock(Artifact.class);
        var file = new File("/somewhere-over-the-rain.bow");
        final DependencyReport dependencyReport = mock(DependencyReport.class);
        lenient().when(localRepositoryManager.getPathForLocalArtifact(any()))
                .thenReturn(file.getAbsolutePath());
        lenient().when(localRepositoryManager.getRepository().getBasedir())
                .thenReturn(new File(""));

        // When
        spy.analyzeCustomPageArtifact(artifact, properties, dependencyReport);

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
    void should_read_customPage_properties_in_archive() throws Exception {
        // Given
        var artifact = mock(Artifact.class);
        File file = getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip");
        when(artifact.getFile()).thenReturn(file);

        Properties expectedProperties = new Properties();
        expectedProperties.load(getClass().getResourceAsStream("/page.properties"));

        // When
        final Properties properties = analyzer.readPageProperties(artifact);

        // Then
        assertThat(properties).isEqualTo(expectedProperties);
    }

    @Test
    void appliesToArchiveArtifact() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "my-rest-api", "0.0.1-SNAPSHOT", "runtime", "zip",
                null, new DefaultArtifactHandler("zip"));
        artifact.setFile(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isTrue();
    }

    @Test
    void doesNotAppliesToJarFile() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-actorfilter-single-user-1.0.0.jar"));

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
            var artifact = new DefaultArtifact("org.bonita.connector", "my-rest-api", "0.0.1-SNAPSHOT", "runtime",
                    "zip", null, new DefaultArtifactHandler("zip"));
            artifact.setFile(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));

            // Then
            assertThat(analyzer.appliesTo(artifact)).isFalse();
        } finally {
            // Reset the reader spy to avoid side effects on other tests
            lenient().doCallRealMethod().when(reader).readFirstEntry(any(), any(), any());
        }
    }

}
