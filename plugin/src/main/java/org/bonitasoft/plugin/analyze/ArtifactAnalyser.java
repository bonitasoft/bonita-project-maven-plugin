package org.bonitasoft.plugin.analyze;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;

public interface ArtifactAnalyser {
	DependencyReport analyse(List<Artifact> artifacts);
}
