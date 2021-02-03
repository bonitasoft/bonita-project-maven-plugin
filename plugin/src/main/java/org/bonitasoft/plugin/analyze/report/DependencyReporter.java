package org.bonitasoft.plugin.analyze.report;

import org.bonitasoft.plugin.analyze.report.model.DependencyReport;

public interface DependencyReporter {
	void report(DependencyReport dependencyReport);
}
