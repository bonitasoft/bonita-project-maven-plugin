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
package org.bonitasoft.plugin.analyze.handler;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipFile;

import javax.xml.bind.JAXBException;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.engine.business.application.exporter.ApplicationNodeContainerConverter;
import org.bonitasoft.engine.business.application.xml.ApplicationNode;
import org.bonitasoft.plugin.analyze.report.model.ApplicationDescriptor;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

class ApplicationDescriptorAnalyzer extends AbstractArtifactAnalyzerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDescriptorAnalyzer.class);
    private static final String APPLICATION_CLASSIFIER = "application";

    private ApplicationNodeContainerConverter converter = new ApplicationNodeContainerConverter();

    ApplicationDescriptorAnalyzer(LocalRepositoryManager localRepositoryManager) {
        super(localRepositoryManager);
    }

    @Override
    public boolean appliesTo(Artifact artifact) {
        File file = artifact.getFile();
        var fileName = file.getName();
        try {
            return file.isFile()
                    && fileName.endsWith(".zip")
                    && Objects.equals(artifact.getClassifier(), APPLICATION_CLASSIFIER)
                    && hasApplicationDescriptor(file);
        } catch (IOException e) {
            LOGGER.warn("An error occured while reading {}", file, e);
            return false;
        }
    }

    @Override
    public DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException {
        var descriptor = readApplicationDescriptorInArchive(artifact.getFile());
        descriptor.ifPresent(app -> report.addApplicationDescriptor(ApplicationDescriptor.create(app.getDisplayName(),
                app.getVersion(),
                app.getDescription(),
                app.getProfile(),
                app.getToken(),
                create(artifact))));
        return report;
    }

    /**
     * Look for the first ApplicationNode found in the artifact file.
     * 
     * @param artifactFile The application zip archive
     * @return an optional ApplicationNode
     * @throws IOException
     */
    Optional<ApplicationNode> readApplicationDescriptorInArchive(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().matches("applications/(.*).xml"))
                .map(entry -> {
                    try (ZipFile zipFile = new ZipFile(artifactFile);
                            var is = zipFile.getInputStream(entry)) {
                        var container = converter.unmarshallFromXML(is.readAllBytes());
                        return container.getApplications().stream().findFirst();
                    } catch (IOException | JAXBException | SAXException e) {
                        LOGGER.warn("Failed to parse {} for application descriptor", artifactFile, e);
                        return null;
                    }
                }).filter(Objects::nonNull).orElseThrow(
                        () -> new IllegalArgumentException(
                                format("No application descriptor found in %s", artifactFile)));
    }

    boolean hasApplicationDescriptor(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().matches("applications/(.*).xml")).isPresent();
    }

}
