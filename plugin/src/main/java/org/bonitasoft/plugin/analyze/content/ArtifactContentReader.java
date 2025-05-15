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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.apache.maven.artifact.Artifact;
import org.slf4j.LoggerFactory;

/**
 * Provides methods to read the content of an artifact (JAR, ZIP or project folder).
 */
public interface ArtifactContentReader {

    public static enum ArtifactFileType {

        /** A compiled .jar file */
        JAR,
        /** A compiled .zip file */
        ZIP,
        /** A maven project folder with source code */
        PROJECT_FOLDER;

        public boolean appliesTo(Artifact artifact) {
            File file = artifact.getFile();
            switch (this) {
                case JAR:
                    return file.isFile() && file.getName().endsWith(".jar");
                case ZIP:
                    return file.isFile() && file.getName().endsWith(".zip");
                case PROJECT_FOLDER:
                    return file.isDirectory();
                default:
                    return false;
            }
        }
    }

    /**
     * A content entry in the artifact.
     * <p>Path is relative to the artifact root.</p>
     * <p>Supplier provides an input stream to read the content.</p>
     * <p>Entry can often no longer be used once the artifact resource is closed,
     * so the supplier works only with read methods.</p>
     */
    public static record Entry(Path path, Supplier<InputStream> supplier) {
    }

    /**
     * Test whether the artifact is of the concerned file type.
     * 
     * @param artifact the artifact to test
     * @return true when the artifact is of the concerned file type
     */
    default boolean appliesTo(Artifact artifact) {
        return getArtifactFileType().appliesTo(artifact);
    }

    /**
     * Get the concerned artifact file type.
     * 
     * @return the artifact file type
     */
    public ArtifactFileType getArtifactFileType();

    /**
     * Test whether the artifact has a content entry satisfying the predicate.
     * 
     * @param artifact the artifact to test (with adequate file type)
     * @param predicateOnPath the predicate testing on entry path
     * @return relative path to a valid entry, empty when no valid entry was found
     */
    default Optional<Path> findEntry(Artifact artifact, Predicate<Path> predicateOnPath) {
        try {
            return readFirstEntry(artifact, predicateOnPath, entry -> entry.path);
        } catch (IOException e) {
            logIOException(e, artifact.getFile(), null);
            return Optional.empty();
        }
    }

    /**
     * Test whether the artifact has a content entry with the given name.
     * 
     * @param artifact the artifact to test (with adequate file type)
     * @param entryName the simple name of the entry to test
     * @return relative path to a valid entry, empty when no valid entry was found
     */
    default Optional<Path> findEntryWithName(Artifact artifact, String entryName) {
        return findEntry(artifact, path -> path.getFileName().toString().equals(entryName));
    }

    /**
     * Test whether the artifact has a content entry satisfying the predicate.
     * 
     * @param artifact the artifact to test (with adequate file type)
     * @param predicateOnPath the predicate testing on entry path
     * @return true when there is a valid entry
     */
    default boolean hasEntryWithPath(Artifact artifact, Predicate<Path> predicateOnPath) {
        return findEntry(artifact, predicateOnPath).isPresent();
    }

    /**
     * Test whether the artifact has a content entry at the given relative path.
     * 
     * @param artifact the artifact to test (with adequate file type)
     * @param entryPath the relative path of the entry to test
     * @return true when there is a valid entry
     */
    default boolean hasEntryWithPath(Artifact artifact, Path entryPath) {
        return findEntry(artifact, path -> path.equals(entryPath)).isPresent();
    }

    /**
     * Read the (compiled) content of the first artifact entry which satisfy the path predicate.
     * <p>You must close the requested input stream in the reader.</p>
     * 
     * @param artifact the artifact to read (with adequate file type)
     * @param predicateOnPath the predicate to test on the entry path
     * @param reader the reading job which will eventually consume the valid entry content and transform it
     * @return the result of the reading job, empty when no valid entry was found
     * @throws IOException exception reading artifact content
     */
    public <T> Optional<T> readFirstEntry(Artifact artifact, Predicate<Path> predicateOnPath, Function<Entry, T> reader)
            throws IOException;

    /**
     * Read the (compiled) content of the artifact entry at the given relative path.
     * <p>The input stream is closed automatically, so you do not have to worry about it.</p>
     * 
     * @param artifact the artifact to read (with adequate file type)
     * @param entryPath the relative path of the entry to read
     * @param reader the reading job which will consume the entry content
     * @throws IOException exception reading artifact content
     * @throws IllegalArgumentException when entry not found
     */
    default void readEntry(Artifact artifact, Path entryPath, Consumer<InputStream> reader)
            throws IllegalArgumentException, IOException {
        var entryRead = readFirstEntry(artifact, entryPath::equals, entry -> {
            try (var is = entry.supplier().get()) {
                if (is != null) {
                    reader.accept(is);
                }
                // else an error probably occurred and has been logged
                return Boolean.valueOf(is != null);
            } catch (IOException e) {
                logIOException(e, artifact.getFile(), entryPath);
                return Boolean.FALSE;
            }
        });
        if (entryRead.isEmpty()) {
            throw new IllegalArgumentException("No entry found for path " + entryPath);
        }
        if (!entryRead.get()) {
            throw new IllegalArgumentException("Entry reading failed for path " + entryPath);
        }
    }

    /**
     * Read the (compiled) content of the artifact entries which satisfy the path predicate.
     * <p>You must close the requested input stream in the reader.</p>
     * 
     * @param artifact the artifact to read (with adequate file type)
     * @param predicateOnPath the predicate to test on the entry path
     * @param reader the reading job which will eventually consume each valid entry content and collect the result
     * @return the result collected by the reader
     * @throws IOException exception reading artifact content
     */
    public <R, A> R readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Collector<Entry, A, R> reader)
            throws IOException;

    /**
     * Read the (compiled) content of the artifact entries which satisfy the path predicate.
     * <p>You must close the requested input stream in the reader.</p>
     * 
     * @param artifact the artifact to read (with adequate file type)
     * @param predicateOnPath the predicate to test on the entry path
     * @param reader the reading job which will eventually consume each valid entry content
     * @throws IOException exception reading artifact content
     */
    default void readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Consumer<Entry> reader)
            throws IOException {
        Collector<Entry, Void, Void> collector = Collector.of(
                () -> null,
                (v, entry) -> reader.accept(entry),
                (v1, v2) -> null);
        readEntries(artifact, predicateOnPath, collector);
    }

    /**
     * Log an IOException when reading the artifact content.
     * 
     * @param e IO exception
     * @param file the artifact file
     * @param entryPath the relative path of the entry or null
     */
    default void logIOException(IOException e, File file, Path entryPath) {
        if (entryPath == null) {
            LoggerFactory.getLogger(ArtifactContentReader.class).warn(
                    "An error occured while reading {}", file, e);
        } else {
            LoggerFactory.getLogger(ArtifactContentReader.class).warn(
                    "An error occured while reading {} (content at {} cannot be read)", file, entryPath, e);
        }
    }

    /**
     * Detect the implementation hierarchy of a java class in the artifact.
     * <p>Some implementations may not support this method for their artifact file type.</p>
     * 
     * @param className the name of the class to analyze
     * @param artifact the artifact to read (with adequate file type)
     * @param exceptionHandler handles any ClassNotFoundException while loading classes
     * @return set of class names in the implementation's parent hierarchy
     * @throws UnsupportedOperationException when the implementation does not support this method
     */
    public Set<String> detectImplementationHierarchy(String className, Artifact artifact,
            Consumer<ClassNotFoundException> exceptionHandler) throws UnsupportedOperationException;

}
