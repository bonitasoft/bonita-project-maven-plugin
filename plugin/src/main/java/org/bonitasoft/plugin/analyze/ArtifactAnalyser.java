package org.bonitasoft.plugin.analyze;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.model.AnalysisResult;

public interface ArtifactAnalyser {
	AnalysisResult analyse(List<Artifact> artifacts);
}
