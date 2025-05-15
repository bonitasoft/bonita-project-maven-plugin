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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javax.xml.bind.JAXBException;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.engine.business.application.exporter.ApplicationNodeContainerConverter;
import org.bonitasoft.engine.business.application.xml.ApplicationNode;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.model.ApplicationDescriptor;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

class ApplicationDescriptorAnalyzer extends AbstractArtifactAnalyzerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDescriptorAnalyzer.class);
    private static final String APPLICATION_CLASSIFIER = "application";
    private static final Predicate<Path> IS_APPLICATION_XML = path -> path.getParent().toString().equals("applications")
            && path.getFileName().toString().matches(".*\\.xml");

    private ApplicationNodeContainerConverter converter = new ApplicationNodeContainerConverter();

    ApplicationDescriptorAnalyzer(LocalRepositoryManager localRepositoryManager, ArtifactContentReader contentReader) {
        super(localRepositoryManager, contentReader);
    }

    @Override
    public boolean appliesTo(Artifact artifact) {
        return super.appliesTo(artifact)
                && Objects.equals(artifact.getClassifier(), APPLICATION_CLASSIFIER)
                && hasApplicationDescriptor(artifact);
    }

    @Override
    public DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException {
        var descriptor = readApplicationDescriptor(artifact);
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
     * @param artifact The artifact to read
     * @return an optional ApplicationNode
     * @throws IOException
     */
    Optional<ApplicationNode> readApplicationDescriptor(Artifact artifact) throws IOException {
        Optional<Optional<ApplicationNode>> appDesc = getContentReader().readFirstEntry(artifact,
                IS_APPLICATION_XML, entry -> {
                    try (var is = entry.supplier().get()) {
                        var container = converter.unmarshallFromXML(is.readAllBytes());
                        return container.getApplications().stream().findFirst();
                    } catch (IOException | JAXBException | SAXException e) {
                        LOGGER.warn("Failed to parse {} for application descriptor", artifact, e);
                        return Optional.empty();
                    }
                });
        return appDesc.orElseThrow(() -> new IllegalArgumentException(
                format("No application descriptor found in %s", artifact.getFile())));
    }

    boolean hasApplicationDescriptor(Artifact artifact) {
        return getContentReader().hasEntryWithPath(artifact, IS_APPLICATION_XML);
    }

}
