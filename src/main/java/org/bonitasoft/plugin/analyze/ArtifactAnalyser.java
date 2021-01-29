package org.bonitasoft.plugin.analyze;

import java.util.List;

import org.apache.maven.artifact.Artifact;

public interface ArtifactAnalyser {
	AnalysisResult analyse(List<Artifact> artifacts);
}
