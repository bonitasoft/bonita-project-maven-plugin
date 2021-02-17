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
package org.bonitasoft.plugin.analyze;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.service.deserializer.classfile.DeserializeClassFileProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.inject.Singleton;

@Named
@Singleton
public class ConnectorResolver {

    public static final String CONNECTOR_TYPE = "org/bonitasoft/engine/connector/Connector";
    public static final String FILTER_TYPE = "org/bonitasoft/engine/filter/UserFilter";
    public static final String ABSTRACT_CONNECTOR_TYPE = "org/bonitasoft/engine/connector/AbstractConnector";
    public static final String ABSTRACT_FILTER_TYPE = "org/bonitasoft/engine/filter/AbstractUserFilter";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorResolver.class);

    private static final String IMPLEMENTATION_EXTENSION = ".impl";

    private static final String DEFINITION_EXTENSION = ".def";

    private static final String IMPLEMENTATION_NS = "http://www.bonitasoft.org/ns/connector/implementation/6.0";

    private static final String DEFINITION_NS = "http://www.bonitasoft.org/ns/connector/definition/6.1";

    private DeserializeClassFileProcessor decompiler = new DeserializeClassFileProcessor();

    public static String readElement(Document document, String elementName) {
        String textContent = document.getElementsByTagName(elementName).item(0).getTextContent();
        if (textContent != null) {
            textContent = textContent.trim();
        }
        return textContent;
    }

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
                    if (hierarchy.contains(CONNECTOR_TYPE) || hierarchy.contains(ABSTRACT_CONNECTOR_TYPE)) {
                        return ConnectorImplementation.create(className,
                                new DescriptorIdentifier(definitionId, definitionVersion),
                                new DescriptorIdentifier(implementationId, implementationVersion),
                                artifact.getFile().getAbsolutePath(),
                                resource.getPath());
                    } else if (hierarchy.contains(FILTER_TYPE) || hierarchy.contains(ABSTRACT_FILTER_TYPE)) {
                        return ActorFilterImplementation.create(className,
                                new DescriptorIdentifier(definitionId, definitionVersion),
                                new DescriptorIdentifier(implementationId, implementationVersion),
                                artifact.getFile().getAbsolutePath(),
                                resource.getPath());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Definition> findAllDefinitions(Artifact artifact) throws IOException {
        return findDefinitionDescriptors(artifact.getFile())
                .stream()
                .map(resource -> {
                    Document document = resource.getDocument();
                    String definitionId = readElement(document, "id");
                    String definitionVersion = readElement(document, "version");
                    return Definition.create(new DescriptorIdentifier(definitionId, definitionVersion),
                            artifact.getFile().getAbsolutePath(),
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
            LOGGER.error(String.format("Failed to read %s in %s.", entry, file), e);
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

    static Optional<JarEntry> findJarEntry(File file, Predicate<? super JarEntry> entryPredicate)
            throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            return jarFile.stream()
                    .filter(entryPredicate)
                    .findFirst();
        }
    }

    public Set<String> detectImplementationHierarchy(String className, File jarFile) {
        DecompilerLoader loader = new DecompilerLoader(jarFile);
        Set<String> hierarchy = new HashSet<>();
        try {
            ClassFile classFile = decompiler.loadClassFile(loader, className);
            String superType = null;
            if (classFile != null) {
                superType = classFile.getSuperTypeName();
                hierarchy.add(classFile.getSuperTypeName());
                if (classFile.getInterfaceTypeNames() != null) {
                    Stream.of(classFile.getInterfaceTypeNames())
                            .filter(Objects::nonNull)
                            .forEach(hierarchy::add);
                }
            }
            while (superType != null
                    && !superType.equals("java/lang/Object")
                    && (!hierarchy.contains(CONNECTOR_TYPE) 
                            || !hierarchy.contains(FILTER_TYPE)
                            || !hierarchy.contains(ABSTRACT_CONNECTOR_TYPE)
                            || !hierarchy.contains(ABSTRACT_FILTER_TYPE))) {
                classFile = decompiler.loadClassFile(loader, toClassName(superType));
                if (classFile != null) {
                    superType = classFile.getSuperTypeName();
                    hierarchy.add(superType);
                    if (classFile.getInterfaceTypeNames() != null) {
                        Stream.of(classFile.getInterfaceTypeNames())
                                .filter(Objects::nonNull)
                                .forEach(hierarchy::add);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Implementation super type resolution failed", e);
        }
        return hierarchy;
    }

    private String toClassName(String type) {
        return type.replace("/", ".");
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

    private static class DecompilerLoader implements Loader {

        private final File jarFile;

        public DecompilerLoader(File jarFile) {
            this.jarFile = jarFile;
        }

        @Override
        public boolean canLoad(String internalName) {
            String classPath = internalName.replace(".", "/") + ".class";
            try {
                return findJarEntry(jarFile, entry -> entry.getName().equals(classPath))
                        .isPresent();
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public byte[] load(String internalName) throws LoaderException {
            String classPath = internalName.replace(".", "/") + ".class";
            try {
                return findJarEntry(jarFile, entry -> entry.getName().equals(classPath))
                        .map(entry -> {
                            try (InputStream in = new JarFile(jarFile).getInputStream(entry)) {
                                return in == null ? new byte[0] : in.readAllBytes();
                            } catch (IOException e) {
                                LOGGER.error(String.format("Failed to read %s in %s", entry, jarFile), e);
                                return new byte[0];
                            }
                        })
                        .orElse(new byte[0]);
            } catch (IOException e) {
                throw new LoaderException(e);
            }
        }
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
