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
package org.bonitasoft.plugin.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.bonitasoft.engine.business.application.exporter.ApplicationNodeContainerConverter;
import org.bonitasoft.engine.business.application.xml.ApplicationNodeContainer;
import org.bonitasoft.engine.business.application.xml.ApplicationPageNode;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

/**
 * This Mojo first detects if Bonita provided pages (i.e. pages from User Application) are used in project applications.
 * Then resolves used pages (i.e. downloads artifacts from a repository).
 * Finally, it copies pages artifacts to an output folder (to be packaged afterward).
 */
@Slf4j
@Mojo(name = "copy-provided-pages", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class CopyProvidedPagesMojo extends AbstractMojo {

    protected static final Map<String, DefaultArtifactCoordinate> PROVIDED_PAGES = Map.of(
            "custompage_tasklist", buildPageArtifactCoordinate("page-user-task-list"), //
            "custompage_processlistBonita", buildPageArtifactCoordinate("page-user-process-list"), //
            "custompage_userCaseListBonita", buildPageArtifactCoordinate("page-user-case-list"), //
            "custompage_userCaseDetailsBonita", buildPageArtifactCoordinate("page-user-case-details"));

    private static final String APPLICATION_SOURCE_DIR = "applications/";
    private static final String PAGE_GROUP_ID = "org.bonitasoft.web.page";
    private static final String PAGE_EXTENSION = "zip";
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project.build.directory}/provided-pages", readonly = true, required = true)
    protected File outputFolder;

    private final ArtifactResolver artifactResolver;

    private final ApplicationNodeContainerConverter converter = new ApplicationNodeContainerConverter();

    @Inject
    public CopyProvidedPagesMojo(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Clean the output folder for every execution of the mojo
        if (outputFolder.exists()) {
            try {
                FileUtils.deleteDirectory(outputFolder);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        String.format("Failed to clean output directory: %s", outputFolder), e);
            }
        }

        List<DefaultArtifactCoordinate> providedPages = detectProvidedPages();
        if (providedPages.isEmpty()) {
            log.debug("No provided pages detected");
            return;
        }

        try {
            Files.createDirectories(outputFolder.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Failed to create output directory: %s", outputFolder), e);
        }

        for (ArtifactCoordinate providedPage : providedPages) {
            copyArtifact(providedPage);
        }
    }

    protected List<DefaultArtifactCoordinate> detectProvidedPages() throws MojoFailureException {
        var appSourceDir = project.getBasedir().toPath().resolve(APPLICATION_SOURCE_DIR);
        if (!Files.exists(appSourceDir) || !Files.isDirectory(appSourceDir)) {
            log.debug("Applications source directory [{}] does not exist or is not a directory", appSourceDir);
            return Collections.emptyList();
        }

        log.info("Detecting provided pages usage in applications...");
        try (Stream<Path> sourcePaths = Files.list(appSourceDir)) {
            var providedPages = sourcePaths
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().matches("^.*\\.xml$"))
                    .map(this::toApplicationContainerNode)
                    .filter(Objects::nonNull)
                    .flatMap(this::listPages)
                    .filter(PROVIDED_PAGES::containsKey)
                    .distinct()
                    .map(PROVIDED_PAGES::get)
                    .map(this::setArtifactVersion)
                    .collect(Collectors.toList());
            log.info("Found the usage of {} provided pages", providedPages.size());
            log.debug("Page artifacts coordinates: {}", providedPages);
            return providedPages;
        } catch (IOException e) {
            throw new MojoFailureException(String.format("Failed to list files in directory: %s", appSourceDir), e);
        }
    }

    protected ApplicationNodeContainer toApplicationContainerNode(Path file) {
        try {
            return converter.unmarshallFromXML(Files.readAllBytes(file));
        } catch (JAXBException | IOException | SAXException e) {
            log.warn("Cannot parse {}. File skipped from provided page detection.", file);
            return null;
        }
    }

    protected Stream<String> listPages(ApplicationNodeContainer applicationNode) {
        return applicationNode.getApplications().stream()
                .flatMap(application -> application.getApplicationPages().stream()
                        .map(ApplicationPageNode::getCustomPage));
    }

    protected DefaultArtifactCoordinate setArtifactVersion(DefaultArtifactCoordinate artifactCoordinate) {
        var version = project.getDependencyManagement().getDependencies().stream()
                .filter(dependency -> dependency.getGroupId().equals(artifactCoordinate.getGroupId())
                        && dependency.getArtifactId().equals(artifactCoordinate.getArtifactId()))
                .findFirst()
                .map(Dependency::getVersion)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Missing declaration of dependency: %s", artifactCoordinate)));
        artifactCoordinate.setVersion(version);
        return artifactCoordinate;
    }

    private void copyArtifact(ArtifactCoordinate artifactCoordinate) throws MojoFailureException {
        var artifact = resolveArtifact(artifactCoordinate);
        try {
            log.info("Copying page artifact '{}' into {}", artifact.getName(), outputFolder);
            Files.copy(artifact.toPath(), outputFolder.toPath().resolve(artifact.getName()));
        } catch (IOException e) {
            throw new MojoFailureException(String.format("Failed to copy artifact: %s", artifactCoordinate), e);
        }
    }

    private File resolveArtifact(ArtifactCoordinate artifactCoordinate) throws MojoFailureException {
        try {
            return artifactResolver.resolveArtifact(session.getProjectBuildingRequest(), artifactCoordinate)
                    .getArtifact().getFile();
        } catch (ArtifactResolverException e) {
            throw new MojoFailureException(String.format("Failed to resolve artifact: %s", artifactCoordinate), e);
        }
    }

    private static DefaultArtifactCoordinate buildPageArtifactCoordinate(String artifactId) {
        var artifact = new DefaultArtifactCoordinate();
        artifact.setGroupId(PAGE_GROUP_ID);
        artifact.setArtifactId(artifactId);
        artifact.setExtension(PAGE_EXTENSION);
        return artifact;
    }
}
