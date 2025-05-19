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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
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

    /**
     * Contains the entry and cleans the filtered descriptor.
     */
    static record EntryAndCleaner(Entry entry, Closeable cleaner) {
    }

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
        return Files.exists(targetPath) && notInTargetDirectory(artifact).test(targetPath);
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
        if (!notInTargetDirectory(artifact).test(sourcePath)) {
            throw new IllegalArgumentException("File " + sourcePath + " is in target build directory");
        }

        var filteredDescriptor = filterDescriptor(baseDir, sourcePath);
        try (var is = Files.newInputStream(filteredDescriptor)) {
            reader.accept(is);
        } finally {
            // delete the filtered descriptor
            Files.deleteIfExists(filteredDescriptor);
        }
    }

    @Override
    public <T> Optional<T> readFirstEntry(Artifact artifact, Predicate<Path> predicateOnPath, Function<Entry, T> reader)
            throws IOException {
        var baseDir = artifact.getFile();
        UnaryOperator<Path> toRelative = baseDir.toPath()::relativize;
        var notInTarget = notInTargetDirectory(artifact);
        // max depth 10 is arbitrary
        BiPredicate<Path, BasicFileAttributes> matcher = (path, attr) -> {
            if (attr.isRegularFile()) {
                return predicateOnPath.and(notInTarget).test(toRelative.apply(path));
            }
            // we should not need directories
            return false;
        };
        try (var pathsStream = Files.find(baseDir.toPath(), 10, matcher)) {
            var entriesStream = pathsStream.map(sourcePath -> makeEntry(baseDir, sourcePath));
            Optional<EntryAndCleaner> firstEntry = entriesStream.findFirst();
            if (firstEntry.isEmpty()) {
                return Optional.empty();
            } else {
                var entry = firstEntry.get();
                try {
                    return Optional.of(reader.apply(entry.entry));
                } finally {
                    entry.cleaner.close();
                }
            }
        }
    }

    @Override
    public <R, A> R readEntries(Artifact artifact, Predicate<Path> predicateOnPath, Collector<Entry, A, R> reader)
            throws IOException {
        var baseDir = artifact.getFile();
        UnaryOperator<Path> toRelative = baseDir.toPath()::relativize;
        var notInTarget = notInTargetDirectory(artifact);
        // max depth 10 is arbitrary
        BiPredicate<Path, BasicFileAttributes> matcher = (path, attr) -> {
            if (attr.isRegularFile()) {
                return predicateOnPath.and(notInTarget).test(toRelative.apply(path));
            }
            // we should not need directories
            return false;
        };
        try (var pathsStream = Files.find(baseDir.toPath(), 10, matcher)) {
            var entriesStream = pathsStream.map(sourcePath -> makeEntry(baseDir, sourcePath));
            List<Closeable> closables = new ArrayList<>();
            try {
                return entriesStream.map(entryWithCleaner -> {
                    closables.add(entryWithCleaner.cleaner);
                    return entryWithCleaner.entry;
                }).collect(reader);
            } finally {
                for (var closable : closables) {
                    closable.close();
                }
            }
        }
    }

    EntryAndCleaner makeEntry(File baseDir, Path sourcePath) {
        // we need the compiled target path for the entry
        Path resolvedPathInTarget = sourcePath.getFileName();

        AtomicReference<Path> filteredDescriptorRef = new AtomicReference<>();
        Entry entry = new Entry(resolvedPathInTarget, () -> {
            try {
                var filteredDescriptor = filterDescriptor(baseDir, sourcePath);
                filteredDescriptorRef.set(filteredDescriptor);
                return Files.newInputStream(filteredDescriptor);
            } catch (IOException e) {
                logIOException(e, baseDir, baseDir.toPath().relativize(sourcePath));
                return null;
            }
        });
        return new EntryAndCleaner(entry, () -> {
            // delete the filtered descriptor
            var path = filteredDescriptorRef.get();
            if (path != null) {
                Files.deleteIfExists(path);
            }
        });
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
        var mavenProject = findMavenProject(basedir);
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
        var mavenProject = findMavenProject(baseDir);

        try {
            List<String> classpathElements = mavenProject.getCompileClasspathElements();
            classpathElements.add(mavenProject.getBuild().getOutputDirectory());
            URL[] urls = classpathElements.stream().map(elt -> {
                try {
                    return new File(elt).toURI().toURL();
                } catch (MalformedURLException e) {
                    LoggerFactory.getLogger(ArtifactContentReader.class).error(
                            "An error occured while loading implementation class {} from Maven project {}", className,
                            baseDir, e);
                    return null;
                }
            }).filter(Objects::nonNull).toArray(URL[]::new);
            return detectImplementationHierarchyFromClasspath(className, exceptionHandler, urls);
        } catch (DependencyResolutionRequiredException | IOException e) {
            LoggerFactory.getLogger(ArtifactContentReader.class).error(
                    "An error occured while loading implementation class {} from Maven project {}", className, baseDir,
                    e);
        }
        return Set.of();
    }

    /**
     * Find the Maven project in the reactor projects list.
     * 
     * @param baseDir the base directory of the project
     * @return the Maven project
     */
    MavenProject findMavenProject(File baseDir) {
        return reactorProjects.stream()
                .filter(p -> Objects.equals(baseDir, p.getBasedir()))
                .findFirst().orElseThrow();
    }

    /**
     * Predicate to filter out the entries in the target directory.
     * 
     * @param artifact the artifact the maven artifact
     * @return the predicate to exclude target directory
     */
    private Predicate<Path> notInTargetDirectory(Artifact artifact) {
        File baseDir = artifact.getFile();
        var mavenProject = findMavenProject(baseDir);
        var targetDir = baseDir.toPath().relativize(Path.of(mavenProject.getBuild().getDirectory()));
        return path -> !path.startsWith(targetDir);
    }

    private Set<String> detectImplementationHierarchyFromClasspath(String className,
            Consumer<ClassNotFoundException> exceptionHandler, URL[] classpathUrls) throws IOException {
        try (var classLoader = new URLClassLoader(classpathUrls, this.getClass().getClassLoader())) {
            var clazz = classLoader.loadClass(className);
            var superClasses = ClassUtils.getAllSuperclasses(clazz);
            var interfaces = ClassUtils.getAllInterfaces(clazz);
            return Stream.concat(superClasses.stream(), interfaces.stream())
                    .map(Class::getName)
                    .collect(Collectors.toSet());
        } catch (ClassNotFoundException e) {
            exceptionHandler.accept(e);
        }
        return Set.of();
    }

}
