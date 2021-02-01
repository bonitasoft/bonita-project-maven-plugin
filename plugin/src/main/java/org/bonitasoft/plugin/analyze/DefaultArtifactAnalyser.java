package org.bonitasoft.plugin.analyze;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.model.AnalysisResult;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Named
@Singleton
public class DefaultArtifactAnalyser implements ArtifactAnalyser {

	private final ConnectorResolver connectorResolver;

	@Inject
	public DefaultArtifactAnalyser(ConnectorResolver connectorResolver) {
		this.connectorResolver = connectorResolver;
	}

	@Override
	public AnalysisResult analyse(List<Artifact> artifacts) {
		AnalysisResult analysisResult = new AnalysisResult();
		artifacts.forEach(artifact -> {
			try {
				analyze(artifact, analysisResult);
			}
			catch (IOException e) {
				throw new AnalysisResultReportException("Failed to analyse artifacts: " + artifact.getId(), e);
			}
		});
		return analysisResult;
	}

	private AnalysisResult analyze(Artifact artifact, AnalysisResult result) throws IOException {
		File artifactFile = artifact.getFile();
		String fileName = artifactFile.getName();
		if (fileName.endsWith(".jar") && hasConnectorDescriptor(artifactFile)) {
			analyseConnectorArtifact(artifact, result);
		}
		if (fileName.endsWith(".zip") && hasCustomPageDescriptor(artifactFile)) {
			analyseCustomPageArtifact(artifact, result);
		}
		return result;
	}

	private void analyseConnectorArtifact(Artifact artifact, AnalysisResult result) throws IOException {
		List<Implementation> allImplementations = connectorResolver.findAllImplementations(artifact);
		List<Definition> allDefinitions = connectorResolver.findAllDefinitions(artifact);
		List<Implementation> connectorImplementations = allImplementations.stream()
				.filter(impl -> ConnectorResolver.ABSTRACT_CONNECTOR_TYPE.equals(impl.getSuperType()))
				.collect(toList());
		List<Implementation> filterImplementations = allImplementations.stream()
				.filter(impl -> ConnectorResolver.ABSTRACT_FILTER_TYPE.equals(impl.getSuperType()))
				.collect(toList());
		allDefinitions.stream()
				.filter(def -> hasMatchingImplementation(def, connectorImplementations))
				.forEach(result::addConnectorDefinition);
		allDefinitions.stream()
				.filter(def -> hasMatchingImplementation(def, filterImplementations))
				.forEach(result::addFilterDefinition);
		connectorImplementations.forEach(result::addConnectorImplementation);
		filterImplementations.forEach(result::addFilterImplementation);
	}

	protected boolean hasMatchingImplementation(Definition def, List<Implementation> connectorImplementations) {
		return connectorImplementations.stream()
				.anyMatch(implementation ->
						Objects.equals(def.getDefinitionId(), implementation.getDefinitionId()) &&
								Objects.equals(def.getDefinitionVersion(), implementation.getDefinitionVersion())
				);
	}

	protected void analyseCustomPageArtifact(Artifact artifact, AnalysisResult result) throws IOException {
		Properties properties = readPageProperties(artifact.getFile());
		String contentType = properties.getProperty("contentType");
		CustomPageType customPageType = CustomPageType.fromValue(contentType);
		switch (customPageType) {
			case FORM:
				String name = properties.getProperty(CustomPage.NAME_PROPERTY);
				String displayName = properties.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
				String description = properties.getProperty(CustomPage.DESCRIPTION_PROPERTY);
				result.addForm(new Form(name, displayName, description, artifact.getFile().getAbsolutePath()));
				break;
			case PAGE:
				String name1 = properties.getProperty(CustomPage.NAME_PROPERTY);
				String displayName1 = properties.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
				String description1 = properties.getProperty(CustomPage.DESCRIPTION_PROPERTY);
				result.addPage(new Page(name1, displayName1, description1, artifact.getFile().getAbsolutePath()));
				break;
			case THEME:
				String name3 = properties.getProperty(CustomPage.NAME_PROPERTY);
				String displayName3 = properties.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
				String description3 = properties.getProperty(CustomPage.DESCRIPTION_PROPERTY);
				result.addTheme(new Theme(name3, displayName3, description3, artifact.getFile().getAbsolutePath()));
				break;
			case API_EXTENSION:
				String name2 = properties.getProperty(CustomPage.NAME_PROPERTY);
				String displayName2 = properties.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
				String description2 = properties.getProperty(CustomPage.DESCRIPTION_PROPERTY);
				result.addRestAPIExtension(new RestAPIExtension(name2, displayName2, description2, artifact.getFile().getAbsolutePath()));
				break;
			default:
				throw new AnalysisResultReportException("Unsupported Custom Page type: " + contentType);
		}
	}

	protected boolean hasConnectorDescriptor(File artifactFile) throws IOException {
		try (JarFile jarFile = new JarFile(artifactFile)) {
			Enumeration<JarEntry> enumOfJar = jarFile.entries();
			while (enumOfJar.hasMoreElements()) {
				JarEntry jarEntry = enumOfJar.nextElement();
				String name = jarEntry.getName();
				if (name.endsWith(".impl")) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean hasCustomPageDescriptor(File artifactFile) throws IOException {
		try (ZipFile zipFile = new ZipFile(artifactFile)) {
			Enumeration<? extends ZipEntry> enumOfZip = zipFile.entries();
			while (enumOfZip.hasMoreElements()) {
				ZipEntry zipEntry = enumOfZip.nextElement();
				String name = zipEntry.getName();
				if (name.equals("page.properties")) {
					return true;
				}
			}
		}
		return false;
	}

	protected Properties readPageProperties(File artifactFile) throws IOException {
		try (ZipFile zipFile = new ZipFile(artifactFile)) {
			Enumeration<? extends ZipEntry> enumOfZip = zipFile.entries();
			while (enumOfZip.hasMoreElements()) {
				ZipEntry zipEntry = enumOfZip.nextElement();
				String name = zipEntry.getName();
				if (name.equals("page.properties")) {
					Properties prop = new Properties();
					prop.load(zipFile.getInputStream(zipEntry));
					return prop;
				}
			}
		}
		throw new IllegalArgumentException(format("No page.properties found in %s", artifactFile));
	}

	public enum CustomPageType {
		FORM("form"), PAGE("page"), THEME("theme"), API_EXTENSION("apiExtension");

		private String value;

		CustomPageType(String value) {
			this.value = value;
		}

		/**
		 * Create enum instance from string representation in property files
		 * @param customPageTypeValue
		 * @return
		 */
		public static CustomPageType fromValue(final String customPageTypeValue) {
			return Arrays.stream(CustomPageType.values())
					.filter(customPageType -> customPageType.getValue().equals(customPageTypeValue)).findFirst()
					.orElseThrow(()-> new AnalysisResultReportException("Unsupported CustomPage type:" + customPageTypeValue));
		}

		public String getValue() {
			return value;
		}
	}
}
