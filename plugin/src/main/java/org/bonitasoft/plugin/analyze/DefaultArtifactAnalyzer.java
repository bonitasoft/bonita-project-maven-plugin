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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.CustomPage.CustomPageType;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

class DefaultArtifactAnalyzer implements ArtifactAnalyzer {

    private static final String CUSTOMPAGE_DESCRIPTOR_PROPERTIES = "page.properties";
    private final ConnectorResolver connectorResolver;
    private IssueCollector issueCollector;
    private LocalRepositoryManager localRepositoryManager;
    private MavenResourcesFiltering mavenResourcesFiltering;
    private List<MavenProject> reactorProjects;

    public DefaultArtifactAnalyzer(ConnectorResolver connectorResolver,
            IssueCollector issueCollector,
            LocalRepositoryManager localRepositoryManager,
            MavenResourcesFiltering mavenResourcesFiltering,
            List<MavenProject> reactorProjects) {
        this.connectorResolver = connectorResolver;
        this.issueCollector = issueCollector;
        this.localRepositoryManager = localRepositoryManager;
        this.mavenResourcesFiltering = mavenResourcesFiltering;
        this.reactorProjects = reactorProjects;
    }

    @Override
    public DependencyReport analyze(List<Artifact> artifacts) {
        DependencyReport dependencyReport = new DependencyReport();
        artifacts.forEach(artifact -> {
            try {
                analyze(artifact, dependencyReport);
            } catch (IOException | MavenFilteringException e) {
                throw new AnalysisResultReportException("Failed to analyse artifacts: " + artifact.getId(), e);
            }
        });
        issueCollector.getIssues().forEach(dependencyReport::addIssue);
        return dependencyReport;
    }

    private DependencyReport analyze(Artifact artifact, DependencyReport result)
            throws IOException, MavenFilteringException {
        File artifactFile = artifact.getFile();
        if (artifactFile.isFile()) {
            String fileName = artifactFile.getName();
            if (fileName.endsWith(".jar") && hasConnectorDescriptor(artifactFile)) {
                analyzeConnectorArtifact(artifact, result);
            }
            if (fileName.endsWith(".zip") && hasCustomPageDescriptor(artifactFile)) {
                var properties = readPagePropertiesInArchive(artifact.getFile());
                analyzeCustomPageArtifact(artifact, properties, result);
            }
        }
        // Project module that might not be installed in local repository yet
        if (artifactFile.isDirectory()) {
            // Search for page.properties descriptor
            var basedir = artifactFile;
            var pageDescriptor = basedir.toPath().resolve(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
            if (!Files.exists(pageDescriptor)) {
                pageDescriptor = basedir.toPath().resolve("src/main/resources")
                        .resolve(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
            }
            if (Files.exists(pageDescriptor)) {
                var filteredPageDescriptor = filterPageDescriptor(basedir, pageDescriptor);
                var properties = readPageProperties(filteredPageDescriptor);
                analyzeCustomPageArtifact(artifact, properties, result);
            }
        }
        return result;
    }

    private Path filterPageDescriptor(File basedir, Path pageDescriptor) throws MavenFilteringException {
        var mavenResource = newFilteredResource(pageDescriptor);
        var mavenResourcesExecution = newMavenResourcesExecution(mavenResource, basedir);
        mavenResourcesFiltering.filterResources(mavenResourcesExecution);
        return mavenResourcesExecution.getOutputDirectory().toPath().resolve(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
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

    private Resource newFilteredResource(Path pageDescriptor) {
        var mavenResource = new Resource();
        mavenResource.setDirectory(pageDescriptor.getParent().toString());
        mavenResource.setIncludes(List.of(pageDescriptor.getFileName().toString()));
        mavenResource.setFiltering(true);
        return mavenResource;
    }

    private void analyzeConnectorArtifact(Artifact artifact, DependencyReport result) throws IOException {
        List<Implementation> allImplementations = connectorResolver.findAllImplementations(artifact, issueCollector);
        List<Definition> allDefinitions = connectorResolver.findAllDefinitions(artifact, issueCollector);
        List<ConnectorImplementation> connectorImplementations = allImplementations.stream()
                .filter(ConnectorImplementation.class::isInstance).map(ConnectorImplementation.class::cast)
                .collect(toList());
        List<ActorFilterImplementation> filterImplementations = allImplementations.stream()
                .filter(ActorFilterImplementation.class::isInstance).map(ActorFilterImplementation.class::cast)
                .collect(toList());
        allDefinitions.stream().filter(def -> hasMatchingImplementation(def, connectorImplementations))
                .forEach(result::addConnectorDefinition);
        allDefinitions.stream().filter(def -> hasMatchingImplementation(def, filterImplementations))
                .forEach(result::addFilterDefinition);
        allDefinitions.stream()
                .filter(def -> !hasMatchingImplementation(def,
                        Stream.of(connectorImplementations, filterImplementations).flatMap(Collection::stream)
                                .collect(Collectors.toList())))
                .map(def -> Issue.create(Type.UNKNOWN_DEFINITION_TYPE, String.format(
                        "%s declares a definition '%s (%s)' but no matching implementation has been found. This definition will be ignored.",
                        def.getJarEntry(), def.getDefinitionId(), def.getDefinitionVersion()), Severity.WARNING,
                        artifact.getId()))
                .forEach(result::addIssue);
        connectorImplementations.forEach(result::addConnectorImplementation);
        filterImplementations.forEach(result::addFilterImplementation);
    }

    protected boolean hasMatchingImplementation(Definition def,
            List<? extends Implementation> connectorImplementations) {
        return connectorImplementations.stream()
                .anyMatch(implementation -> Objects.equals(def.getDefinitionId(), implementation.getDefinitionId())
                        && Objects.equals(def.getDefinitionVersion(), implementation.getDefinitionVersion()));
    }

    protected void analyzeCustomPageArtifact(Artifact artifact, Properties pageDescriptor, DependencyReport result) {
        String contentType = pageDescriptor.getProperty("contentType");
        CustomPageType customPageType = CustomPageType.valueOf(contentType.toUpperCase());
        String name = pageDescriptor.getProperty(CustomPage.NAME_PROPERTY);
        String displayName = pageDescriptor.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
        String description = pageDescriptor.getProperty(CustomPage.DESCRIPTION_PROPERTY);
        switch (customPageType) {
            case FORM:
                result.addForm(Form.create(name, displayName, description, create(artifact)));
                break;
            case PAGE:
                result.addPage(Page.create(name, displayName, description, create(artifact)));
                break;
            case THEME:
                result.addTheme(Theme.create(name, displayName, description, create(artifact)));
                break;
            case APIEXTENSION:
                result.addRestAPIExtension(RestAPIExtension.create(name, displayName, description, create(artifact)));
                break;
            default:
                throw new AnalysisResultReportException("Unsupported Custom Page type: " + contentType);
        }
    }

    private org.bonitasoft.plugin.analyze.report.model.Artifact create(Artifact artifact) {
        return org.bonitasoft.plugin.analyze.report.model.Artifact.create(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getBaseVersion() == null ? artifact.getVersion() : artifact.getBaseVersion(),
                artifact.getClassifier(),
                artifactLocalRepositoryFile(artifact));
    }

    private String artifactLocalRepositoryFile(Artifact artifact) {
        var artifactPath = localRepositoryManager
                .getPathForLocalArtifact(new DefaultArtifact(artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getType(),
                        artifact.getBaseVersion() == null ? artifact.getVersion() : artifact.getBaseVersion()));
        var localRepositoryPath = localRepositoryManager.getRepository().getBasedir().toPath();
        return localRepositoryPath.resolve(artifactPath).toAbsolutePath().toString();
    }

    protected boolean hasConnectorDescriptor(File artifactFile) throws IOException {
        return findJarEntry(artifactFile, entry -> entry.getName().endsWith(".impl")).isPresent();
    }

    protected boolean hasCustomPageDescriptor(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().equals(CUSTOMPAGE_DESCRIPTOR_PROPERTIES))
                .isPresent();
    }

    private static Optional<? extends ZipEntry> findZipEntry(File file, Predicate<? super ZipEntry> entryPredicate)
            throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            return zipFile.stream().filter(entryPredicate).findFirst();
        }
    }

    Properties readPagePropertiesInArchive(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().equals(CUSTOMPAGE_DESCRIPTOR_PROPERTIES))
                .map(entry -> {
                    try (ZipFile zipFile = new ZipFile(artifactFile);
                            Reader reader = new InputStreamReader(zipFile.getInputStream(entry),
                                    StandardCharsets.UTF_8)) {
                        Properties prop = new Properties();
                        prop.load(reader);
                        return prop;
                    } catch (IOException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).orElseThrow(
                        () -> new IllegalArgumentException(format("No page.properties found in %s", artifactFile)));
    }

    Properties readPageProperties(Path pageDescriptor) throws IOException {
        try (var in = new InputStreamReader(
                new FileInputStream(pageDescriptor.toFile()), StandardCharsets.UTF_8)) {
            Properties prop = new Properties();
            prop.load(in);
            return prop;
        } finally {
            Files.delete(pageDescriptor);
        }
    }

    static Optional<JarEntry> findJarEntry(File file, Predicate<? super JarEntry> entryPredicate) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.stream().filter(entryPredicate).findFirst();
        }
    }

}
