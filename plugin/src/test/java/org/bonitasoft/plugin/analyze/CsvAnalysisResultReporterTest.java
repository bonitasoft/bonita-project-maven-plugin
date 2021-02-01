package org.bonitasoft.plugin.analyze;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import org.bonitasoft.plugin.analyze.report.CsvAnalysisResultReporter;
import org.bonitasoft.plugin.analyze.report.model.AnalysisResult;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;

class CsvAnalysisResultReporterTest {

	@TempDir
	File tempDir;

	private CsvAnalysisResultReporter reporter;

	private File outputFile;

	@BeforeEach
	void setUp() {
		outputFile = new File(tempDir, "report.csv");
		reporter = new CsvAnalysisResultReporter(outputFile);
	}

	@Test
	void should_report_csv() throws URISyntaxException {
		// Given
		final AnalysisResult analysisResult = new AnalysisResult();
		final String connectorArtifact = newArtifact("org.my.connector", "a-connector", "1.0.0");
		analysisResult.addConnectorImplementation(new Implementation(
				"aClassName",
				"anIimplementationId",
				"anImplementationVersion",
				"aDefinitionId",
				"aDefinitionVersion",
				"aPath",
				connectorArtifact
		));
		analysisResult.addConnectorDefinition(new Definition(
				"aDdefinitionId",
				"aDefinitionVersion",
				"anEntryPath",
				connectorArtifact
		));

		final String filterArtifact = newArtifact("org.my.filter", "a-filter", "1.0.0");
		analysisResult.addFilterImplementation(new Implementation(
				"aClassName",
				"anIimplementationId",
				"anImplementationVersion",
				"aDefinitionId",
				"aDefinitionVersion",
				"aPath",
				filterArtifact
		));
		analysisResult.addFilterDefinition(new Definition(
				"aDdefinitionId",
				"aDefinitionVersion",
				"anEntryPath",
				filterArtifact
		));

		Properties properties2 = newProperties("rest");
		String name2 = properties2.getProperty(CustomPage.NAME_PROPERTY);
		String displayName2 = properties2.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
		String description2 = properties2.getProperty(CustomPage.DESCRIPTION_PROPERTY);
		analysisResult.addRestAPIExtension(new RestAPIExtension(name2, displayName2, description2, newArtifact("org.my.rest", "a-rest", "1.0.0")));

		Properties properties1 = newProperties("page");
		String name1 = properties1.getProperty(CustomPage.NAME_PROPERTY);
		String displayName1 = properties1.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
		String description1 = properties1.getProperty(CustomPage.DESCRIPTION_PROPERTY);
		analysisResult.addPage(new Page(name1, displayName1, description1, newArtifact("org.my.page", "a-page", "1.0.0")));

		Properties properties = newProperties("form");
		String name = properties.getProperty(CustomPage.NAME_PROPERTY);
		String displayName = properties.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
		String description = properties.getProperty(CustomPage.DESCRIPTION_PROPERTY);
		analysisResult.addForm(new Form(name, displayName, description, newArtifact("org.my.form", "a-form", "1.0.0")));

		Properties properties3 = newProperties("theme");
		String name3 = properties3.getProperty(CustomPage.NAME_PROPERTY);
		String displayName3 = properties3.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
		String description3 = properties3.getProperty(CustomPage.DESCRIPTION_PROPERTY);
		analysisResult.addTheme(new Theme(name3, displayName3, description3, newArtifact("org.my.theme", "a-theme", "1.0.0")));

		// When
		reporter.report(analysisResult);

		// Then
		final File expectedContent = getResourceAsFile("/expected-report.csv");
		assertThat(outputFile).exists().isFile().hasSameBinaryContentAs(expectedContent);
	}

	private String newArtifact(String groupId, String artifactId, String version) {
		return artifactId + "-" + version + ".jar";
	}

	private Properties newProperties(String prefix) {
		final Properties properties = new Properties();
		properties.setProperty(CustomPage.NAME_PROPERTY, prefix + "_name");
		properties.setProperty(CustomPage.DESCRIPTION_PROPERTY, prefix + "_desc");
		properties.setProperty(CustomPage.DISPLAY_NAME_PROPERTY, prefix + "_display_name");
		return properties;
	}
}
