package org.bonitasoft.plugin.analyze.report;

import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.junit.jupiter.api.Test;

abstract class AbstractDependencyReportReporterTest {

	protected abstract DependencyReporter getReporter();

	@Test
	void should_write_report() throws Exception {
		// Given
		final DependencyReport dependencyReport = new DependencyReport();
		final String connectorArtifactFile = "/tmp/a-connector-1.0.0.jar";
		dependencyReport.addConnectorImplementation(ConnectorImplementation.create(
				"aClassName",
				new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
				new DescriptorIdentifier("anImplementationId", "anImplementationVersion"),
				connectorArtifactFile,
				"connector.impl"
		));
		dependencyReport.addConnectorDefinition(Definition.create(
				new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
				connectorArtifactFile,
				"connector.def"
		));

		final String filterArtifactFile = "/tmp/a-filter-1.0.0.jar";
		dependencyReport.addFilterImplementation(ActorFilterImplementation.create(
				"aClassName",
				new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
                new DescriptorIdentifier("anImplementationId", "anImplementationVersion"),
				filterArtifactFile,
				"filter.impl"
		));
		dependencyReport.addFilterDefinition(Definition.create(
		        new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
				filterArtifactFile,
				"filter.def"
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
