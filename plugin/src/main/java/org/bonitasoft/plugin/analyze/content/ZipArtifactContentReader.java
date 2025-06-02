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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;

/**
 * Reads the content of a ZIP file.
 * <p>Java classes can not be loaded.</p>
 */
public class ZipArtifactContentReader implements ArtifactContentReader {

    @Override
    public ArtifactFileType getArtifactFileType() {
        return ArtifactFileType.ZIP;
    }

    private Path toPath(ZipEntry zipEntry) {
        return Path.of(URI.create(zipEntry.getName()).toString());
    }

    @Override
    public <T> Optional<T> readFirstEntry(Artifact artifact, Predicate<Path> predicateOnPath, Function<Entry, T> reader)
            throws IOException {
        var file = artifact.getFile();
        try (ZipFile zipFile = new ZipFile(file)) {
            var zipEntriesStream = zipFile.stream()
                    .filter(zipEntry -> predicateOnPath.test(toPath(zipEntry)));
            return zipEntriesStream.findFirst().map(zipEntry -> {
                Entry entry = makeEntry(file, zipFile, zipEntry);
                return reader.apply(entry);
            });
        }
    }

    @Override
    public <R, A> R readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Collector<Entry, A, R> reader)
            throws IOException {
        var file = artifact.getFile();
        try (ZipFile zipFile = new ZipFile(file)) {
            var zipEntriesStream = zipFile.stream()
                    .filter(zipEntry -> predicateOnPath.test(toPath(zipEntry)));
            var entriesStream = zipEntriesStream.map(zipEntry -> makeEntry(file, zipFile, zipEntry));
            return entriesStream.collect(reader);
        }
    }

    private Entry makeEntry(File file, ZipFile zipFile, ZipEntry zipEntry) {
        return new Entry(toPath(zipEntry), () -> {
            try {
                return zipFile.getInputStream(zipEntry);
            } catch (IOException e) {
                logIOException(e, file, toPath(zipEntry));
                return null;
            }
        });
    }

    @Override
    public Set<String> detectImplementationHierarchy(String className, Artifact artifact,
            Consumer<ClassNotFoundException> exceptionHandler) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Cannot detect implementation hierarchy in a ZIP file");
    }

}
