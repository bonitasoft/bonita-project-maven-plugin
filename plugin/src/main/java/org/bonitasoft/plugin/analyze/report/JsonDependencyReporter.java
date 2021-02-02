package org.bonitasoft.plugin.analyze.report;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;

public class JsonDependencyReporter implements DependencyReporter {

	private final ObjectMapper mapper = new ObjectMapper()
			.findAndRegisterModules()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setSerializationInclusion(Include.NON_NULL)
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private final File outputFile;

	public JsonDependencyReporter(File outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public void report(DependencyReport dependencyReport) {
		try {
			final File parentFile = outputFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			mapper.writeValue(outputFile, dependencyReport);
		}
		catch (IOException e) {
			throw new AnalysisResultReportException("Failed to generate report", e);
		}
	}
}
