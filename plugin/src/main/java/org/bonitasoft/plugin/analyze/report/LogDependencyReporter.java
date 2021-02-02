package org.bonitasoft.plugin.analyze.report;

import org.apache.maven.plugin.logging.Log;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Implementation;

import static java.lang.String.format;

public class LogDependencyReporter implements DependencyReporter {

	private final Log log;

	public LogDependencyReporter(Log log) {
		this.log = log;
	}

	private static String asString(CustomPage customPage) {
		return customPage.getName() + " in file " + customPage.getFilePath();
	}

	private static String asString(Definition definition) {
		return definition.getDefinitionId() + "-" + definition.getDefinitionVersion() + " in file " + definition.getFilePath();
	}

	private static String asString(Implementation implementation) {
		return implementation.getImplementationId() + "-" + implementation.getImplementationVersion()
				+ " for " + implementation.getDefinitionId() + "-" + implementation.getDefinitionVersion()
				+ " in file " + implementation.getFilePath();
	}

	@Override
	public void report(DependencyReport dependencyReport) {
		log.info(format("=== %s Connector definitions found ===", dependencyReport.getConnectorDefinitions().size()));
		dependencyReport.getConnectorDefinitions().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Connector implementations found ===", dependencyReport.getConnectorImplementations().size()));
		dependencyReport.getConnectorImplementations().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Actor filter definitions found ===", dependencyReport.getFilterDefinitions().size()));
		dependencyReport.getFilterDefinitions().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Actor filter implementations found ===", dependencyReport.getFilterImplementations().size()));
		dependencyReport.getFilterImplementations().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Pages found ===", dependencyReport.getPages().size()));
		dependencyReport.getPages().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Forms found", dependencyReport.getForms().size()));
		dependencyReport.getForms().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Rest API Extensions found ===", dependencyReport.getRestApiExtensions().size()));
		dependencyReport.getRestApiExtensions().stream().map(LogDependencyReporter::asString).forEach(log::info);

		log.info(format("=== %s Themes found ===", dependencyReport.getThemes().size()));
		dependencyReport.getThemes().stream().map(LogDependencyReporter::asString).forEach(log::info);
	}
}
