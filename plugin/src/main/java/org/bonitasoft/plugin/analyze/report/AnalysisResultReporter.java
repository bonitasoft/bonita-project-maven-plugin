package org.bonitasoft.plugin.analyze.report;

import org.bonitasoft.plugin.analyze.report.model.AnalysisResult;

public interface AnalysisResultReporter {
	void report(AnalysisResult analysisResult);
}
