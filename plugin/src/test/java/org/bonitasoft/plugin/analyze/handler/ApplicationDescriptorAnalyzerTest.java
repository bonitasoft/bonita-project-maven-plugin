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
import static org.assertj.core.api.Assertions.tuple;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;

import java.io.File;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationDescriptorAnalyzerTest {

    @InjectMocks
    ApplicationDescriptorAnalyzer analyzer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    LocalRepositoryManager localRepositoryManager;

    @Test
    void appliesToArchiveWithApplicationDescriptor() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.application", "bonita-user-application", "1.0.0", "runtime",
                "zip",
                "application", new DefaultArtifactHandler("zip"));
        artifact.setFile(getResourceAsFile("/application.zip"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isTrue();
    }

    @Test
    void doesNotAppliesToArchiveWithoutValidFile() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.application", "bonita-user-application", "1.0.0", "runtime",
                "zip",
                "application", new DefaultArtifactHandler("zip"));
        artifact.setFile(new File("doesNotExists"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isFalse();
    }

    @Test
    void doesNotAppliesToFolderArtifact() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.application", "bonita-user-application", "1.0.0", "runtime",
                "zip",
                "application", new DefaultArtifactHandler("zip"));
        artifact.setFile(getResourceAsFile("/application_without_descriptor.zip"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isFalse();
    }

    @Test
    void doesNotAppliesToArchiveWithoutApplicationsFile() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.application", "bonita-user-application", "1.0.0", "runtime",
                "zip",
                "application", new DefaultArtifactHandler("zip"));
        artifact.setFile(getResourceAsFile("/application_without_descriptor2.zip"));

        // Then
        assertThat(analyzer.appliesTo(artifact)).isFalse();
    }

    @Test
    void analyzeApplicationDescriptor() throws Exception {
        // Given
        var artifact = new DefaultArtifact("org.bonita.application", "bonita-user-application", "1.0.0", "runtime",
                "zip",
                "application", new DefaultArtifactHandler("zip"));
        artifact.setFile(getResourceAsFile("/application.zip"));

        // When
        var result = analyzer.analyze(artifact, new DependencyReport());

        // Then
        assertThat(result.getApplicationDescriptors())
                .extracting("appToken", "displayName", "version", "profile")
                .containsOnly(tuple("userAppBonita", "Bonita User Application", "5.0", "User"));
    }
}
