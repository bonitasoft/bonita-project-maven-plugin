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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectArtifactContentReaderTest {

    ProjectArtifactContentReader projArtifactContentReader;
    Artifact artifact;

    @BeforeEach
    void setUp() throws Exception {
        projArtifactContentReader = spy(new ProjectArtifactContentReader(mock(), mock()));

        artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(getResourceAsFile("/validation/application_zips_unzipped/application"));
    }

    @Test
    void should_apply_to_folder_and_only_to_folder() throws Exception {
        // given setUp, then
        assertThat(projArtifactContentReader.appliesTo(artifact)).isTrue();

        // given
        var jarArtifact = mock(Artifact.class);
        when(jarArtifact.getFile()).thenReturn(getResourceAsFile("/bonita-actorfilter-single-user-1.0.0.jar"));
        // then
        assertThat(projArtifactContentReader.appliesTo(jarArtifact)).isFalse();

        // given
        var zipArtifact = mock(Artifact.class);
        when(zipArtifact.getFile()).thenReturn(getResourceAsFile("/my-rest-api-0.0.1-SNAPSHOT.zip"));
        // then
        assertThat(projArtifactContentReader.appliesTo(zipArtifact)).isFalse();
    }

    @Test
    void should_read_entry() throws IOException, URISyntaxException {
        // given setUp and
        doAnswer(invoc -> invoc.getArguments()[1]).when(projArtifactContentReader).filterDescriptor(any(), any());
        // when
        projArtifactContentReader.readEntry(artifact, Path.of("applications", "bonita-user-application.xml"), is -> {
            // then
            try {
                assertThat(is).isNotNull();
                assertThat(new String(is.readAllBytes()))
                        .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
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
                () -> projArtifactContentReader.readEntry(artifact, Path.of("test", "not_a_file.txt"), is -> {
                    throw new RuntimeException("Should not be called");
                }));
    }

    // #detectImplementationHierarchy can not be tested easily without a real maven project. Tested in IT only.
}
