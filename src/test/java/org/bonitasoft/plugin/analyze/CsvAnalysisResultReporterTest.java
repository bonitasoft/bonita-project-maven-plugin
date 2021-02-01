package org.bonitasoft.plugin.analyze;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Definition;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Form;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Implementation;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Page;
import org.bonitasoft.plugin.analyze.BonitaArtifact.RestAPIExtension;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Theme;
import org.bonitasoft.plugin.analyze.report.CsvAnalysisResultReporter;
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
		final DefaultArtifact connectorArtifact = newArtifact("org.my.connector", "a-connector", "1.0.0");
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

		final DefaultArtifact filterArtifact = newArtifact("org.my.filter", "a-filter", "1.0.0");
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

		analysisResult.addRestAPIExtension(RestAPIExtension.create(
				newProperties("rest"),
				newArtifact("org.my.rest", "a-rest", "1.0.0")
		));

		analysisResult.addPage(Page.create(
				newProperties("page"),
				newArtifact("org.my.page", "a-page", "1.0.0")
		));

		analysisResult.addForm(Form.create(
				newProperties("form"),
				newArtifact("org.my.form", "a-form", "1.0.0")
		));

		analysisResult.addTheme(Theme.create(
				newProperties("theme"),
				newArtifact("org.my.theme", "a-theme", "1.0.0")
		));

		// When
		reporter.report(analysisResult);

		// Then
		final File expectedContent = getResourceAsFile("/expected-report.csv");
		assertThat(outputFile).exists().isFile().hasSameBinaryContentAs(expectedContent);
	}

	private DefaultArtifact newArtifact(String groupId, String artifactId, String version) {
		final DefaultArtifact artifact = new DefaultArtifact(
				groupId, artifactId, version, "compile", "jar", null,
				new DefaultArtifactHandler("jar")
		);
		artifact.setFile(new File(artifact.getArtifactId() + "-" + artifact.getVersion() + "." + artifact.getType()));
		return artifact;
	}

	private Properties newProperties(String prefix) {
		final Properties properties = new Properties();
		properties.setProperty(BonitaArtifact.NAME_PROPERTY, prefix + "_name");
		properties.setProperty(BonitaArtifact.DESCRIPTION_PROPERTY, prefix + "_desc");
		properties.setProperty(BonitaArtifact.DISPLAY_NAME_PROPERTY, prefix + "_display_name");
		return properties;
	}
}
