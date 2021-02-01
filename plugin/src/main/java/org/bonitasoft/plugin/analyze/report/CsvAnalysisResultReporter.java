package org.bonitasoft.plugin.analyze.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.bonitasoft.plugin.analyze.report.model.AnalysisResult;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.Implementation;

import static java.lang.String.format;

public class CsvAnalysisResultReporter implements AnalysisResultReporter {

	public static final String PAGE_LINE_TPL = "%s,%s,%s,%s%n";

	public static final String DEF_LINE_TPL = "%s,%s,%s,%s,%s%n";

	public static final String IMPL_LINE_TPL = "%s,%s,%s,%s,%s,%s,%s,%s%n";

	private final File outputFile;

	public CsvAnalysisResultReporter(File outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public void report(AnalysisResult analysisResult) {
		try {
			Files.createDirectories(outputFile.getParentFile().toPath());
			Files.deleteIfExists(outputFile.toPath());
			Files.createFile(outputFile.toPath());
			try (FileWriter fileWriter = new FileWriter(outputFile);
				 PrintWriter printWriter = new PrintWriter(fileWriter)) {
				analysisResult.getConnectorImplmentations().forEach(impl -> writeAsCsv("CONNECTOR_IMPLEMENTATION", impl, printWriter));
				analysisResult.getFilterImplmentations().forEach(impl -> writeAsCsv("FILTER_IMPLEMENTATION", impl, printWriter));
				analysisResult.getConnectorDefinitions().forEach(def -> writeAsCsv("CONNECTOR_DEFINITION", def, printWriter));
				analysisResult.getFilterDefinitions().forEach(def -> writeAsCsv("FILTER_DEFINITION", def, printWriter));
				analysisResult.getPages().forEach(page -> writeAsCsv("PAGE", page, printWriter));
				analysisResult.getForms().forEach(page -> writeAsCsv("FORM", page, printWriter));
				analysisResult.getRestApiExtensions().forEach(page -> writeAsCsv("REST_API_EXTENSION", page, printWriter));
				analysisResult.getThemes().forEach(page -> writeAsCsv("THEME", page, printWriter));
			}
		}
		catch (IOException e) {
			throw new AnalysisResultReportException(format("Failed to write csv report to file %s", outputFile), e);
		}
	}


	private PrintWriter writeAsCsv(String pageName, CustomPage page, PrintWriter printWriter) {
		return printWriter.printf(PAGE_LINE_TPL,
				pageName,
				page.getName(),
				page.getDisplayName(),
				page.getFilePath());
	}

	private PrintWriter writeAsCsv(String implementationName, Implementation impl, PrintWriter printWriter) {
		return printWriter.printf(IMPL_LINE_TPL,
				implementationName,
				impl.getImplementationId(),
				impl.getImplementationVersion(),
				impl.getClassName(),
				impl.getDefinitionId(),
				impl.getDefinitionVersion(),
				impl.getPath(),
				impl.getFilePath());

	}

	private PrintWriter writeAsCsv(String definitionName, Definition def, PrintWriter printWriter) {
		return printWriter.printf(DEF_LINE_TPL,
				definitionName,
				def.getDefinitionId(),
				def.getDefinitionVersion(),
				def.getEntryPath(),
				def.getFilePath());
	}

}
