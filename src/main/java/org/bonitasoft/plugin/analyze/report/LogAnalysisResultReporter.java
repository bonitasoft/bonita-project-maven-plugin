package org.bonitasoft.plugin.analyze.report;

import org.apache.maven.plugin.logging.Log;
import org.bonitasoft.plugin.analyze.AnalysisResult;

import static java.lang.String.format;

public class LogAnalysisResultReporter implements AnalysisResultReporter {

	private final Log log;

	public LogAnalysisResultReporter(Log log) {
		this.log = log;
	}

	@Override
	public void report(AnalysisResult analysisResult) {
		log.info(format("=== %s Connector definitions found ===", analysisResult.getConnectorDefinitions().size()));
		analysisResult.getConnectorDefinitions().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Connector implementations found ===", analysisResult.getConnectorImplmentations().size()));
		analysisResult.getConnectorImplmentations().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Actor filter definitions found ===", analysisResult.getFilterDefinitions().size()));
		analysisResult.getFilterDefinitions().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Actor filter implementations found ===", analysisResult.getFilterImplmentations().size()));
		analysisResult.getFilterImplmentations().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Pages found ===", analysisResult.getPages().size()));
		analysisResult.getPages().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Forms found", analysisResult.getForms().size()));
		analysisResult.getForms().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Rest API Extensions found ===", analysisResult.getRestApiExtensions().size()));
		analysisResult.getRestApiExtensions().stream().map(Object::toString).forEach(log::info);

		log.info(format("=== %s Themes found ===", analysisResult.getThemes().size()));
		analysisResult.getThemes().stream().map(Object::toString).forEach(log::info);
	}
}
