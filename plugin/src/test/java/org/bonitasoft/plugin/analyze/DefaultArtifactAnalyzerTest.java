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
package org.bonitasoft.plugin.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.handler.ArtifactAnalyzerHandler;
import org.junit.jupiter.api.Test;

class DefaultArtifactAnalyzerTest {

    @Test
    void should_apply_handler() throws IOException, URISyntaxException {
        // Given
        var handler = mock(ArtifactAnalyzerHandler.class);
        when(handler.appliesTo(any())).thenReturn(true);
        var handlers = List.of(handler);
        var analyzer = new DefaultArtifactAnalyzer(handlers);
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

        // When
        var dependencyReport = analyzer.analyze(List.of(artifact));

        // Then
        verify(handler).analyze(eq(artifact), notNull());
        assertThat(dependencyReport).isNotNull();
    }

    @Test
    void should_not_apply_handler() throws IOException, URISyntaxException {
        // Given
        var handler = mock(ArtifactAnalyzerHandler.class);
        when(handler.appliesTo(any())).thenReturn(false);
        var handlers = List.of(handler);
        var analyzer = new DefaultArtifactAnalyzer(handlers);
        var artifact = new DefaultArtifact("org.bonita.connector", "bonita-connector-email", "1.3.0", "runtime", "jar",
                null, new DefaultArtifactHandler("jar"));
        artifact.setFile(getResourceAsFile("/bonita-connector-email-1.3.0.jar"));

        // When
        var dependencyReport = analyzer.analyze(List.of(artifact));

        // Then
        verify(handler, never()).analyze(eq(artifact), notNull());
        assertThat(dependencyReport).isNotNull();
    }

}
