/** 
 * Copyright (C) 2020 BonitaSoft S.A.
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
package org.bonitasoft.plugin.analyze.connector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.ConnectorResolver;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Named
public class ConnectorResolverImpl implements ConnectorResolver {

    private static final String CONNECTOR_TYPE = "org.bonitasoft.engine.connector.Connector";
    private static final String FILTER_TYPE = "org.bonitasoft.engine.filter.UserFilter";
    private static final String ABSTRACT_CONNECTOR_TYPE = "org.bonitasoft.engine.connector.AbstractConnector";
    private static final String ABSTRACT_FILTER_TYPE = "org.bonitasoft.engine.filter.AbstractUserFilter";

    private static final Set<String> FILTER_TYPES = Set.of(FILTER_TYPE, ABSTRACT_FILTER_TYPE);
    private static final Set<String> CONNECTOR_TYPES = Set.of(CONNECTOR_TYPE, ABSTRACT_CONNECTOR_TYPE);

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorResolverImpl.class);

    private static final String IMPLEMENTATION_EXTENSION = ".impl";

    private static final String DEFINITION_EXTENSION = ".def";

    private static final String IMPLEMENTATION_NS = "http://www.bonitasoft.org/ns/connector/implementation/6.0";

    private static final String DEFINITION_NS = "http://www.bonitasoft.org/ns/connector/definition/6.1";

    private static String readElement(Document document, String elementName) {
        String textContent = document.getElementsByTagName(elementName).item(0).getTextContent();
        if (textContent != null) {
            textContent = textContent.trim();
        }
        return textContent;
    }

    @Override
    public List<Implementation> findAllImplementations(Artifact artifact, ArtifactContentReader reader,
            Issue.Collector issueCollector)
            throws IOException {
        return findImplementationDescriptors(artifact, reader, issueCollector)
                .stream()
                .map(resource -> {
                    Document document = resource.getDocument();
                    String className = readElement(document, "implementationClassname");

                    String implementationId = readElement(document, "implementationId");
                    String implementationVersion = readElement(document, "implementationVersion");
                    String definitionId = readElement(document, "definitionId");
                    String definitionVersion = readElement(document, "definitionVersion");
                    Set<String> hierarchy = detectImplementationHierarchy(className, artifact, reader,
                            resource.getPath(), issueCollector);
                    if (!Collections.disjoint(hierarchy, CONNECTOR_TYPES)) {
                        return ConnectorImplementation.create(className,
                                new DescriptorIdentifier(definitionId, definitionVersion),
                                new DescriptorIdentifier(implementationId, implementationVersion),
                                create(artifact),
                                resource.getPath());
                    } else if (!Collections.disjoint(hierarchy, FILTER_TYPES)) {
                        return ActorFilterImplementation.create(className,
                                new DescriptorIdentifier(definitionId, definitionVersion),
                                new DescriptorIdentifier(implementationId, implementationVersion),
                                create(artifact),
                                resource.getPath());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull).toList();
    }

    private static org.bonitasoft.plugin.analyze.report.model.Artifact create(Artifact artifact) {
        return org.bonitasoft.plugin.analyze.report.model.Artifact.create(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getBaseVersion() == null ? artifact.getVersion() : artifact.getBaseVersion(),
                artifact.getClassifier(),
                artifact.getFile().getAbsolutePath());
    }

    @Override
    public List<Definition> findAllDefinitions(Artifact artifact, ArtifactContentReader reader,
            Issue.Collector issueCollector) throws IOException {
        return findDefinitionDescriptors(artifact, reader, issueCollector)
                .stream()
                .map(resource -> {
                    Document document = resource.getDocument();
                    String definitionId = readElement(document, "id");
                    String definitionVersion = readElement(document, "version");
                    return Definition.create(new DescriptorIdentifier(definitionId, definitionVersion),
                            create(artifact),
                            resource.getPath());
                }).toList();
    }

    private List<DocumentResource> findImplementationDescriptors(Artifact artifact, ArtifactContentReader reader,
            Issue.Collector issueCollector)
            throws IOException {
        return getDocumentResources(artifact, reader, IMPLEMENTATION_EXTENSION, IMPLEMENTATION_NS, issueCollector);
    }

    private List<DocumentResource> findDefinitionDescriptors(Artifact artifact, ArtifactContentReader reader,
            Issue.Collector issueCollector)
            throws IOException {
        return getDocumentResources(artifact, reader, DEFINITION_EXTENSION, DEFINITION_NS, issueCollector);
    }

    private List<DocumentResource> getDocumentResources(Artifact artifact, ArtifactContentReader reader,
            String extension,
            String namespace, Issue.Collector issueCollector) throws IOException {
        List<DocumentResource> result = new ArrayList<>();
        reader.readEntries(artifact, path -> path.getFileName().toString().endsWith(extension), entry -> {
            try (InputStream is = entry.supplier().get()) {
                var document = asXMLDocument(is, namespace);
                if (document != null) {
                    result.add(new DocumentResource(entry.path().toString(), document));
                } else {
                    issueCollector.addIssue(Issue.create(Type.INVALID_DESCRIPTOR_FILE,
                            String.format("%s is not compliant with '%s' XML schema definition", entry.path(),
                                    namespace),
                            Severity.ERROR,
                            artifact.getId()));
                }
            } catch (ParserConfigurationException e) {
                LOGGER.error("Failed to parser connector descriptor", e);
            } catch (SAXException e) {
                issueCollector.addIssue(Issue.create(Type.INVALID_DESCRIPTOR_FILE,
                        String.format("%s is not a valid XML file: %s", entry.path(), e.toString()), Severity.ERROR,
                        artifact.getId()));
            } catch (IOException e) {
                LOGGER.error("Failed to read {} in {}.", entry.path(), artifact.getFile(), e);
            }
        });
        return result;
    }

    private Set<String> detectImplementationHierarchy(String className, Artifact artifact, ArtifactContentReader reader,
            String resourcePath, Issue.Collector issueCollector) {
        return reader.detectImplementationHierarchy(className, artifact, e -> {
            LOGGER.error("Failed to load class {} from jar {}", className, artifact.getFile(), e);
            issueCollector.addIssue(Issue.create(Type.INVALID_DESCRIPTOR_FILE,
                    String.format("%s declares an unknown 'implementationClassname': %s", resourcePath, className),
                    Severity.ERROR, artifact.getId()));
        });
    }

    private Document asXMLDocument(InputStream source, String namespace)
            throws ParserConfigurationException, SAXException, IOException {
        if (source != null) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(source));
            Node firstChild = document.getFirstChild();
            String namespaceURI = firstChild.getNamespaceURI();
            if (namespace.equals(namespaceURI)) {
                return document;
            }
        }
        return null;
    }

    public static class DocumentResource {

        private final String path;

        private final Document document;

        public DocumentResource(String path, Document document) {
            this.path = path;
            this.document = document;
        }

        public String getPath() {
            return path;
        }

        public Document getDocument() {
            return document;
        }

    }

}
