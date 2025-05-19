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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JarArtifactContentReaderTest {

    JarArtifactContentReader jarArtifactContentReader = new JarArtifactContentReader();
    Artifact artifact;

    @BeforeEach
    void setUp() throws URISyntaxException {
        artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(getResourceAsFile("/bonita-actorfilter-single-user-1.0.0.jar"));
    }

    @Test
    void should_apply_to_jar_and_only_to_jar() throws Exception {
        // given setUp, then
        assertThat(jarArtifactContentReader.appliesTo(artifact)).isTrue();

        // given
        var zipArtifact = mock(Artifact.class);
        when(zipArtifact.getFile()).thenReturn(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));
        // then
        assertThat(jarArtifactContentReader.appliesTo(zipArtifact)).isFalse();

        // given
        var folderArtifact = mock(Artifact.class);
        when(folderArtifact.getFile())
                .thenReturn(getResourceAsFile("/validation/application_zips_unzipped/application"));
        // then
        assertThat(jarArtifactContentReader.appliesTo(folderArtifact)).isFalse();
    }

    @Test
    void should_read_top_entry() throws IOException {
        // given setUp,
        // when
        jarArtifactContentReader.readEntry(artifact, Path.of("bonita-actorfilter-single-user.properties"), is -> {
            // then
            try {
                assertThat(is).isNotNull();
                assertThat(new String(is.readAllBytes())).startsWith("organization.category=Organization");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void should_read_inner_entry() throws IOException {
        // given setUp,
        // when
        jarArtifactContentReader.readEntry(artifact, Path.of("META-INF", "MANIFEST.MF"), is -> {
            // then
            try {
                assertThat(is).isNotNull();
                assertThat(new String(is.readAllBytes())).startsWith("Manifest-Version: 1.0");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void should_read_all_entries() throws IOException {
        // given setUp,
        List<Path> pathsFound = new ArrayList<>();
        // when
        jarArtifactContentReader.readEntries(artifact, path -> true, entry -> {
            pathsFound.add(entry.path());
        });
        // then
        assertThat(pathsFound).contains(Path.of("META-INF", "MANIFEST.MF"),
                Path.of("bonita-actorfilter-single-user.properties"));
    }

    @Test
    void should_collect_on_no_entry() throws IOException {
        // given setUp,
        // when
        var result = jarArtifactContentReader.readEntries(artifact, Path.of("not_a_file")::equals,
                Collectors.counting());
        // then
        assertThat(result).isZero();
    }

    @Test
    void should_throw_exception_when_read_absent_entry() {
        // given setUp,
        var path = Path.of("test", "not_a_file.txt");
        // then
        assertThrows(IllegalArgumentException.class,
                () -> jarArtifactContentReader.readEntry(artifact, path, is -> {
                    throw new RuntimeException("Should not be called");
                }));

    }

}
