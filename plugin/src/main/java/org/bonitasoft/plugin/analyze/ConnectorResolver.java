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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.model.Definition;
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

@Named
@Singleton
public class ConnectorResolver {

	public static final String ABSTRACT_CONNECTOR_TYPE = "org/bonitasoft/engine/connector/AbstractConnector";

	public static final String ABSTRACT_FILTER_TYPE = "org/bonitasoft/engine/filter/AbstractUserFilter";

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorResolver.class);

	private static final String IMPLEMENTATION_EXTENSION = ".impl";

	private static final String DEFINITION_EXTENSION = ".def";

	private static final String IMPLEMENTATION_NS = "http://www.bonitasoft.org/ns/connector/implementation/6.0";

	private static final String DEFINITION_NS = "http://www.bonitasoft.org/ns/connector/definition/6.1";

	private DeserializeClassFileProcessor decompiler = new DeserializeClassFileProcessor();

	public static String readElement(Document document, String elementName) {
		return document.getElementsByTagName(elementName).item(0).getTextContent();
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
					return Implementation.create(className ,definitionId, definitionVersion, implementationId, implementationVersion, resource.getPath(), artifact.getFile().getAbsolutePath(),null);
				})
				.map(implementation -> detectImplementationType(implementation, artifact.getFile()))
				.collect(Collectors.toList());
	}

	public List<Definition> findAllDefinitions(Artifact artifact) throws IOException {
		return findDefinitionDescriptors(artifact.getFile())
				.stream()
				.map(resource -> {
					Document document = resource.getDocument();
					String definitionId = readElement(document, "id");
					String definitionVersion = readElement(document, "version");
					return Definition.create(definitionId, definitionVersion, resource.getPath(), artifact.getFile().getAbsolutePath());
				})
				.collect(Collectors.toList());
	}

	private List<DocumentResource> findImplementationDescriptors(File artifactFile) throws IOException {
		return getDocumentResources(artifactFile, IMPLEMENTATION_EXTENSION, IMPLEMENTATION_NS);
	}

	private List<DocumentResource> findDefinitionDescriptors(File artifactFile) throws IOException {
		return getDocumentResources(artifactFile, DEFINITION_EXTENSION, DEFINITION_NS);
	}

	private List<DocumentResource> getDocumentResources(File artifactFile, String implementationExtension, String implementationNs) throws IOException {
		List<DocumentResource> descriptors = new ArrayList<>();
		try (JarFile jarFile = new JarFile(artifactFile)) {
			Enumeration<JarEntry> enumOfJar = jarFile.entries();
			while (enumOfJar.hasMoreElements()) {
				JarEntry jarEntry = enumOfJar.nextElement();
				String name = jarEntry.getName();
				if (name.endsWith(implementationExtension)) {
					try (InputStream is = jarFile.getInputStream(jarEntry)) {
						Document document = asXMLDocument(is, implementationNs);
						if (document != null) {
							descriptors.add(new DocumentResource(jarEntry.toString(), document));
						}
					}
				}
			}
		}
		return descriptors;
	}


	public Implementation detectImplementationType(Implementation implementation, File jarFile) {
		String implementationClassName = implementation.getClassName();
		DecompilerLoader loader = new DecompilerLoader(jarFile);
		try {
			ClassFile classFile = decompiler.loadClassFile(loader, implementationClassName);
			String superType = null;
			if (classFile != null) {
				superType = classFile.getSuperTypeName();
			}
			while (superType != null
					&& !ABSTRACT_CONNECTOR_TYPE.equals(superType)
					&& !ABSTRACT_FILTER_TYPE.equals(superType)) {
				classFile = decompiler.loadClassFile(loader, superType);
				if (classFile != null) {
					superType = classFile.getSuperTypeName();
				}
			}
			implementation.setSuperType(superType);
		}
		catch (Exception e) {
			LOGGER.error("Implementation super type resolution failed", e);
		}
		return implementation;
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
		}
		catch (SAXException | IOException | ParserConfigurationException e) {
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
				return finfJarEntry(classPath) != null;
			}
			catch (IOException e) {
				return false;
			}
		}

		@Override
		public byte[] load(String internalName) throws LoaderException {
			String classPath = internalName.replace(".", "/") + ".class";
			try (InputStream in = new JarFile(jarFile).getInputStream(finfJarEntry(classPath))) {
				return in == null ? new byte[0] : in.readAllBytes();
			}
			catch (IOException e) {
				throw new LoaderException(e);
			}
		}

		private JarEntry finfJarEntry(String path) throws IOException {
			try (JarFile jarJarFile = new JarFile(jarFile)) {
				Enumeration<JarEntry> enumOfJar = jarJarFile.entries();
				while (enumOfJar.hasMoreElements()) {
					JarEntry jarEntry = enumOfJar.nextElement();
					String name = jarEntry.getName();
					if (name.equals(path)) {
						return jarEntry;
					}
				}
			}
			return null;
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
