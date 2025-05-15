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
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.slf4j.LoggerFactory;

/**
 * Reads the content of a maven project folder.
 * <p>Java classes are loaded from build directory.</p>
 */
public class ProjectArtifactContentReader implements ArtifactContentReader {

    private MavenResourcesFiltering mavenResourcesFiltering;
    private List<MavenProject> reactorProjects;

    public ProjectArtifactContentReader(MavenResourcesFiltering mavenResourcesFiltering,
            List<MavenProject> reactorProjects) {
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.reactorProjects = reactorProjects;
    }

    @Override
    public ArtifactFileType getArtifactFileType() {
        return ArtifactFileType.PROJECT_FOLDER;
    }

    @Override
    public boolean hasEntryWithPath(Artifact artifact, Path entryPath) {
        // direct search is much quicker than default implementation exploring the file tree
        var baseDir = artifact.getFile();
        var targetPath = baseDir.toPath().resolve(entryPath);
        return Files.exists(targetPath);
    }

    @Override
    public void readEntry(Artifact artifact, Path entryPath, Consumer<InputStream> reader)
            throws IllegalArgumentException, IOException {
        // direct search is much quicker than default implementation exploring the file tree
        var baseDir = artifact.getFile();
        var sourcePath = baseDir.toPath().resolve(entryPath);
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("File " + sourcePath + " does not exist");
        }
        reader.accept(makeInputStream(baseDir, sourcePath));
    }

    private InputStream makeInputStream(File baseDir, Path sourcePath) throws IOException {
        var filteredDescriptor = filterDescriptor(baseDir, sourcePath);
        var is = Files.newInputStream(filteredDescriptor);
        // delegate everything to the original input stream, but customize the close method
        return new InputStream() {

            @Override
            public void close() throws IOException {
                is.close();
                // delete the filtered descriptor
                Files.deleteIfExists(filteredDescriptor);
            }

            @Override
            public int read() throws IOException {
                return is.read();
            }

            @Override
            public int available() throws IOException {
                return is.available();
            }

            @Override
            public synchronized void mark(int readlimit) {
                is.mark(readlimit);
            }

            @Override
            public boolean markSupported() {
                return is.markSupported();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return is.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return is.read(b, off, len);
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                return is.readAllBytes();
            }

            @Override
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                return is.readNBytes(b, off, len);
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                return is.readNBytes(len);
            }

            @Override
            public synchronized void reset() throws IOException {
                is.reset();
            }

            @Override
            public long skip(long n) throws IOException {
                return is.skip(n);
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                is.skipNBytes(n);
            }

            @Override
            public long transferTo(OutputStream out) throws IOException {
                return is.transferTo(out);
            }

        };
    }

    @Override
    public <T> Optional<T> readFirstEntry(Artifact artifact, Predicate<Path> predicateOnPath, Function<Entry, T> reader)
            throws IOException {
        var baseDir = artifact.getFile();
        Function<Path, Path> toRelative = baseDir.toPath()::relativize;
        // max depth 10 is arbitrary
        BiPredicate<Path, BasicFileAttributes> matcher = (path, attr) -> {
            if (attr.isRegularFile()) {
                return predicateOnPath.test(toRelative.apply(path));
            }
            // we should not need directories
            return false;
        };
        try (var pathsStream = Files.find(baseDir.toPath(), 10, matcher)) {
            var entriesStream = pathsStream.map(sourcePath -> makeEntry(baseDir, sourcePath));
            return entriesStream.findFirst().map(entry -> reader.apply(entry));
        }
    }

    @Override
    public <R, A> R readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Collector<Entry, A, R> reader)
            throws IOException {
        var baseDir = artifact.getFile();
        Function<Path, Path> toRelative = baseDir.toPath()::relativize;
        // max depth 10 is arbitrary
        BiPredicate<Path, BasicFileAttributes> matcher = (path, attr) -> {
            if (attr.isRegularFile()) {
                return predicateOnPath.test(toRelative.apply(path));
            }
            // we should not need directories
            return false;
        };
        try (var pathsStream = Files.find(baseDir.toPath(), 10, matcher)) {
            var entriesStream = pathsStream.map(sourcePath -> makeEntry(baseDir, sourcePath));
            return entriesStream.collect(reader);
        }
    }

    private Entry makeEntry(File baseDir, Path sourcePath) {
        Path relPath = baseDir.toPath().relativize(sourcePath);
        Entry entry = new Entry(relPath, () -> {
            try {
                return makeInputStream(baseDir, sourcePath);
            } catch (IOException e) {
                logIOException(e, baseDir, relPath);
                return null;
            }
        });
        return entry;
    }

    Path filterDescriptor(File basedir, Path descriptor) throws IOException {
        var mavenResource = newFilteredResource(descriptor);
        var mavenResourcesExecution = newMavenResourcesExecution(mavenResource, basedir);
        try {
            mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        } catch (MavenFilteringException e) {
            throw new IOException(e);
        }
        return mavenResourcesExecution.getOutputDirectory().toPath().resolve(descriptor.getFileName());
    }

    private MavenResourcesExecution newMavenResourcesExecution(Resource resource, File basedir) {
        var mavenResourcesExecution = new MavenResourcesExecution();
        mavenResourcesExecution.setResources(List.of(resource));
        var mavenProject = reactorProjects.stream()
                .filter(p -> Objects.equals(basedir, p.getBasedir()))
                .findFirst().orElseThrow();
        mavenResourcesExecution.setMavenProject(mavenProject);
        var outputFolder = new File(mavenProject.getBuild().getDirectory());
        mavenResourcesExecution.setOutputDirectory(outputFolder);
        mavenResourcesExecution.setUseDefaultFilterWrappers(true);
        mavenResourcesExecution.setFilterWrappers(List.of());
        mavenResourcesExecution.setEncoding("UTF-8");
        mavenResourcesExecution.setPropertiesEncoding("UTF-8");
        return mavenResourcesExecution;
    }

    private Resource newFilteredResource(Path descriptor) {
        var mavenResource = new Resource();
        mavenResource.setDirectory(descriptor.getParent().toString());
        mavenResource.setIncludes(List.of(descriptor.getFileName().toString()));
        mavenResource.setFiltering(true);
        return mavenResource;
    }

    @Override
    public Set<String> detectImplementationHierarchy(String className, Artifact artifact,
            Consumer<ClassNotFoundException> exceptionHandler) throws UnsupportedOperationException {
        var baseDir = artifact.getFile();
        var mavenProject = reactorProjects.stream()
                .filter(p -> Objects.equals(baseDir, p.getBasedir()))
                .findFirst().orElseThrow();

        try {
            List<String> classpathElements = mavenProject.getCompileClasspathElements();
            classpathElements.add(mavenProject.getBuild().getOutputDirectory());
            URL[] urls = classpathElements.stream().map(elt -> {
                try {
                    return new File(elt).toURI().toURL();
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return null;
                }
            }).filter(Objects::nonNull).toArray(URL[]::new);
            try (var classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
                var clazz = classLoader.loadClass(className);
                var superClasses = ClassUtils.getAllSuperclasses(clazz);
                var interfaces = ClassUtils.getAllInterfaces(clazz);
                return Stream.concat(superClasses.stream(), interfaces.stream())
                        .map(Class::getName)
                        .collect(Collectors.toSet());
            } catch (ClassNotFoundException e) {
                exceptionHandler.accept(e);
            }
        } catch (DependencyResolutionRequiredException | IOException e) {
            LoggerFactory.getLogger(ArtifactContentReader.class).error(
                    "An error occured while loading implementation class {} from Maven project {}", className, baseDir,
                    e);
        }
        return Set.of();
    }

}
