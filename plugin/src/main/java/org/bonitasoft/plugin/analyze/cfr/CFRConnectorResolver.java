/**
 * Copyright (C) 2020 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze.cfr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.bonitasoft.plugin.analyze.ConnectorResolver;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Named
public class CFRConnectorResolver implements ConnectorResolver {

    private static final String CONNECTOR_TYPE = "org.bonitasoft.engine.connector.Connector";
    private static final String FILTER_TYPE = "org.bonitasoft.engine.filter.UserFilter";
    private static final String ABSTRACT_CONNECTOR_TYPE = "org.bonitasoft.engine.connector.AbstractConnector";
    private static final String ABSTRACT_FILTER_TYPE = "org.bonitasoft.engine.filter.AbstractUserFilter";

    private static final Set<String> FILTER_TYPES = Set.of(FILTER_TYPE, ABSTRACT_FILTER_TYPE);
    private static final Set<String> CONNECTOR_TYPES = Set.of(CONNECTOR_TYPE, ABSTRACT_CONNECTOR_TYPE);

    private static final Logger LOGGER = LoggerFactory.getLogger(CFRConnectorResolver.class);

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
    public List<Implementation> findAllImplementations(Artifact artifact) throws IOException {
        return findImplementationDescriptors(artifact.getFile())
                .stream()
                .map(resource -> {
                    Document document = resource.getDocument();
                    String className = readElement(document, "implementationClassname");
                    String implementationId = readElement(document, "implementationId");
                    String implementationVersion = readElement(document, "implementationVersion");
                    String definitionId = readElement(document, "definitionId");
                    String definitionVersion = readElement(document, "definitionVersion");
                    Set<String> hierarchy = detectImplementationHierarchy(className, artifact.getFile());
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
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static org.bonitasoft.plugin.analyze.report.model.Artifact create(Artifact artifact) {
        return org.bonitasoft.plugin.analyze.report.model.Artifact.create(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getClassifier(),
                artifact.getFile().getAbsolutePath());
    }

    @Override
    public List<Definition> findAllDefinitions(Artifact artifact) throws IOException {
        return findDefinitionDescriptors(artifact.getFile())
                .stream()
                .map(resource -> {
                    Document document = resource.getDocument();
                    String definitionId = readElement(document, "id");
                    String definitionVersion = readElement(document, "version");
                    return Definition.create(new DescriptorIdentifier(definitionId, definitionVersion),
                            create(artifact),
                            resource.getPath());
                })
                .collect(Collectors.toList());
    }

    private List<DocumentResource> findImplementationDescriptors(File artifactFile) throws IOException {
        return getDocumentResources(artifactFile, IMPLEMENTATION_EXTENSION, IMPLEMENTATION_NS);
    }

    private List<DocumentResource> findDefinitionDescriptors(File artifactFile) throws IOException {
        return getDocumentResources(artifactFile, DEFINITION_EXTENSION, DEFINITION_NS);
    }

    private List<DocumentResource> getDocumentResources(File artifactFile, String extension,
            String namespace) throws IOException {
        return findJarEntries(artifactFile, entry -> entry.getName().endsWith(extension))
                .stream()
                .map(entry -> createDocumentResource(artifactFile, entry, namespace))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private DocumentResource createDocumentResource(File file, JarEntry entry, String implementationNs) {
        try (JarFile jarFile = new JarFile(file);
                InputStream is = jarFile.getInputStream(entry)) {
            Document document = asXMLDocument(is, implementationNs);
            if (document != null) {
                return new DocumentResource(entry.toString(), document);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read {} in {}.", entry, file, e);
            return null;
        }
        return null;
    }

    static List<JarEntry> findJarEntries(File file, Predicate<? super JarEntry> entryPredicate)
            throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.stream()
                    .filter(entryPredicate)
                    .collect(Collectors.toList());
        }
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

    private Set<String> detectImplementationHierarchy(String className, File file) {
        Set<String> hierarchy = new HashSet<>();
        LOGGER.debug("Resolving connector type for {} in {}", className, file);
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
            LOGGER.error("Failed to load class {} from jar {}", className, file, e);
        }

        return hierarchy;
    }

    private Document asXMLDocument(InputStream source, String namespace) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(source));
            Node firstChild = document.getFirstChild();
            String namespaceURI = firstChild.getNamespaceURI();
            if (namespace.equals(namespaceURI)) {
                return document;
            }
        } catch (SAXException | IOException | ParserConfigurationException e) {
            return null;
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
