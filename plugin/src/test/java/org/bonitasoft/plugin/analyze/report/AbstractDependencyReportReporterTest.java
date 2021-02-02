package org.bonitasoft.plugin.analyze.report;

import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.junit.jupiter.api.Test;

public abstract class AbstractDependencyReportReporterTest {

	protected abstract DependencyReporter getReporter();

	@Test
	void should_write_report() throws Exception {
		// Given
		final DependencyReport dependencyReport = new DependencyReport();
		final String connectorArtifactFile = "/tmp/a-connector-1.0.0.jar";
		dependencyReport.addConnectorImplementation(Implementation.create(
				"aClassName",
				"anIimplementationId",
				"anImplementationVersion",
				"aDefinitionId",
				"aDefinitionVersion",
				"aPath",
				connectorArtifactFile,
				null
		));
		dependencyReport.addConnectorDefinition(Definition.create(
				"aDdefinitionId",
				"aDefinitionVersion",
				"anEntryPath",
				connectorArtifactFile
		));

		final String filterArtifactFile = "/tmp/a-filter-1.0.0.jar";
		dependencyReport.addFilterImplementation(Implementation.create(
				"aClassName",
				"anIimplementationId",
				"anImplementationVersion",
				"aDefinitionId",
				"aDefinitionVersion",
				"aPath",
				filterArtifactFile,
				null
		));
		dependencyReport.addFilterDefinition(Definition.create(
				"aDdefinitionId",
				"aDefinitionVersion",
				"anEntryPath",
				filterArtifactFile
		));

		dependencyReport.addRestAPIExtension(RestAPIExtension.create("rest_name", "rest_displayName", "rest_description", "/tmp/a-rest-1.0.0.jar"));
		dependencyReport.addPage(Page.create("page_name", "page_displayName", "page_description", "/tmp/a-page-1.0.0.jar"));
		dependencyReport.addForm(Form.create("form_name", "form_displayName", "form_description", "/tmp/a-form-1.0.0.jar"));
		dependencyReport.addTheme(Theme.create("theme_name", "theme_displayName", "theme_description", "/tmp/a-theme-1.0.0.jar"));

		// When
		getReporter().report(dependencyReport);

		// Then
		assertReportIsValid();
	}

	protected abstract void assertReportIsValid() throws Exception;

}
