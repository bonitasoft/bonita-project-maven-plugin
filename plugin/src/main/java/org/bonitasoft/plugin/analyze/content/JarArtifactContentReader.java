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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collector;

import org.apache.maven.artifact.Artifact;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.getopt.GetOptSinkFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

/**
 * Reads the content of a JAR file.
 * <p>Java classes are loaded using the CFR library.</p>
 */
public class JarArtifactContentReader implements ArtifactContentReader {

    @Override
    public ArtifactFileType getArtifactFileType() {
        return ArtifactFileType.JAR;
    }

    private Path toPath(JarEntry jarEntry) {
        return Path.of(URI.create(jarEntry.getName()).toString());
    }

    @Override
    public <T> Optional<T> readFirstEntry(Artifact artifact, Predicate<Path> predicateOnPath, Function<Entry, T> reader)
            throws IOException {
        var file = artifact.getFile();
        try (JarFile jarFile = new JarFile(file)) {
            var jarEntriesStream = jarFile.stream()
                    .filter(jarEntry -> predicateOnPath.test(toPath(jarEntry)));
            return jarEntriesStream.findFirst().map(jarEntry -> {
                Entry entry = makeEntry(file, jarFile, jarEntry);
                return reader.apply(entry);
            });
        }
    }

    @Override
    public <R, A> R readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Collector<Entry, A, R> reader)
            throws IOException {
        var file = artifact.getFile();
        try (JarFile jarFile = new JarFile(file)) {
            var jarEntriesStream = jarFile.stream()
                    .filter(jarEntry -> predicateOnPath.test(toPath(jarEntry)));
            var entriesStream = jarEntriesStream.map(jarEntry -> makeEntry(file, jarFile, jarEntry));
            return entriesStream.collect(reader);
        }
    }

    @Override
    public void readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Consumer<Entry> reader)
            throws IOException {
        var file = artifact.getFile();
        try (JarFile jarFile = new JarFile(file)) {
            var entriesStream = jarFile.stream()
                    .filter(jarEntry -> predicateOnPath.test(toPath(jarEntry)));
            entriesStream.forEach(jarEntry -> {
                Entry entry = makeEntry(file, jarFile, jarEntry);
                reader.accept(entry);
            });
        }
    }

    private Entry makeEntry(File file, JarFile jarFile, JarEntry jarEntry) {
        return new Entry(toPath(jarEntry), () -> {
            try {
                return jarFile.getInputStream(jarEntry);
            } catch (IOException e) {
                logIOException(e, file, toPath(jarEntry));
                return null;
            }
        });
    }

    @Override
    public Set<String> detectImplementationHierarchy(String className, Artifact artifact,
            Consumer<ClassNotFoundException> exceptionHandler) throws UnsupportedOperationException {
        Set<String> hierarchy = new HashSet<>();
        var file = artifact.getFile();
        GetOptSinkFactory<Options> factory = OptionsImpl.getFactory();
        Options options = factory.create(Map.of());
        ClassFileSourceImpl classFileSource = new ClassFileSourceImpl(options);
        classFileSource.informAnalysisRelativePathDetail(null, null);
        DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
        Map<Integer, List<JavaTypeInstance>> types = dcCommonState.explicitlyLoadJar(file.getAbsolutePath(),
                AnalysisType.JAR);

        try {
            var classFile = loadClassFile(dcCommonState, types, className);
            if (classFile != null) {
                classFile.getBindingSupers().getBoundSuperClasses().keySet().stream()
                        .map(JavaRefTypeInstance::getRawName)
                        .forEach(hierarchy::add);
            }
        } catch (ClassNotFoundException e) {
            exceptionHandler.accept(e);
        }
        return hierarchy;
    }

    private ClassFile loadClassFile(DCCommonState dcCommonState, Map<Integer, List<JavaTypeInstance>> loadedTypes,
            String className) throws ClassNotFoundException {
        return loadedTypes.values().stream()
                .flatMap(List<JavaTypeInstance>::stream)
                .filter(type -> Objects.equals(className, type.toString()))
                .map(dcCommonState::getClassFile)
                .findFirst()
                .orElseThrow(() -> new ClassNotFoundException(className));
    }

}
