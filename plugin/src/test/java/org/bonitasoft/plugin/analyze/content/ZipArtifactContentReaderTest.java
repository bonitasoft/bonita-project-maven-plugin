/** 
 * Copyright (C) 2025 BonitaSoft S.A.
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
package org.bonitasoft.plugin.analyze.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZipArtifactContentReaderTest {

    ZipArtifactContentReader zipArtifactContentReader = new ZipArtifactContentReader();
    Artifact artifact;

    @BeforeEach
    void setUp() throws URISyntaxException {
        artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));
    }

    @Test
    void should_apply_to_zip_and_only_to_zip() throws Exception {
        // given setUp, then
        assertThat(zipArtifactContentReader.appliesTo(artifact)).isTrue();

        // given
        var jarArtifact = mock(Artifact.class);
        when(jarArtifact.getFile()).thenReturn(getResourceAsFile("/bonita-actorfilter-single-user-1.0.0.jar"));
        // then
        assertThat(zipArtifactContentReader.appliesTo(jarArtifact)).isFalse();

        // given
        var folderArtifact = mock(Artifact.class);
        when(folderArtifact.getFile())
                .thenReturn(getResourceAsFile("/validation/application_zips_unzipped/application"));
        // then
        assertThat(zipArtifactContentReader.appliesTo(folderArtifact)).isFalse();
    }

    @Test
    void should_read_entry() throws IOException, URISyntaxException {
        // given setUp,
        // when
        zipArtifactContentReader.readEntry(artifact, Path.of("page.properties"), is -> {
            // then
            try {
                assertThat(is).isNotNull();
                assertThat(new String(is.readAllBytes())).contains("name=custompage_myRestApi");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void should_throw_exception_when_read_absent_entry() throws IOException, URISyntaxException {
        // given setUp,
        // then
        assertThrows(IllegalArgumentException.class,
                () -> zipArtifactContentReader.readEntry(artifact, Path.of("test", "not_a_file.txt"), is -> {
                    throw new RuntimeException("Should not be called");
                }));

    }

}
