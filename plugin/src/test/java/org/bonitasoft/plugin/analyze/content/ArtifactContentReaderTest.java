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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader.Entry;
import org.junit.jupiter.api.Test;

public class ArtifactContentReaderTest {

    ArtifactContentReader artifactContentReader = new ArtifactContentReader() {

        @Override
        public ArtifactFileType getArtifactFileType() {
            return null;
        }

        @Override
        public <T> Optional<T> readFirstEntry(Artifact artifact, Predicate<Path> predicateOnPath,
                Function<Entry, T> reader) throws IOException {
            return Optional.empty();
        }

        @Override
        public <R, A> R readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Collector<Entry, A, R> reader)
                throws IOException {
            return null;
        }

        @Override
        public Set<String> detectImplementationHierarchy(String className, Artifact artifact,
                Consumer<ClassNotFoundException> exceptionHandler) throws UnsupportedOperationException {
            return null;
        }
    };

    @Test
    void should_hasEntryWithPath_call_readFirstEntry() throws Exception {
        // given
        var spy = spy(artifactContentReader);
        Path anyPath = Path.of("somewhere/anywhere");
        when(spy.readFirstEntry(any(), any(), any())).thenReturn(Optional.of(anyPath));

        // when
        var found = spy.hasEntryWithPath(mock(), anyPath);

        // then
        assertThat(found).isTrue();
        verify(spy).readFirstEntry(any(), any(), any());
    }

    @Test
    void should_throw_IllegalArgumentException_exception_when_reading_entry_fails()
            throws IOException, URISyntaxException {
        // given
        var spy = spy(artifactContentReader);
        Path anyPath = Path.of("somewhere/anywhere");
        when(spy.readFirstEntry(any(), any(), any())).thenReturn(Optional.of(false));

        // then
        assertThrows(IllegalArgumentException.class,
                () -> spy.readEntry(mock(), anyPath, is -> {
                }),
                "Entry reading failed for path " + anyPath.toString());

    }

    @Test
    void should_log_IOException_when_reading_entry_fails() throws IOException, URISyntaxException {
        // given
        var spy = spy(artifactContentReader);
        Path anyPath = Path.of("somewhere/anywhere");
        InputStream inputStream = mock(InputStream.class);
        IOException ioException = new IOException("test exception");
        var artifactFile = mock(File.class);
        var artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(artifactFile);
        doThrow(ioException).when(inputStream).close();
        doAnswer(invocation -> {
            Function<Entry, Boolean> reader = invocation.getArgument(2);
            Entry entry = new Entry(anyPath, () -> inputStream);
            return Optional.of(reader.apply(entry));
        }).when(spy).readFirstEntry(any(), any(), any());

        // when, then
        assertThrows(IllegalArgumentException.class,
                () -> spy.readEntry(artifact, anyPath, is -> {
                }),
                "Entry reading failed for path " + anyPath.toString());
        verify(spy).logIOException(ioException, artifactFile, anyPath);
    }

}
