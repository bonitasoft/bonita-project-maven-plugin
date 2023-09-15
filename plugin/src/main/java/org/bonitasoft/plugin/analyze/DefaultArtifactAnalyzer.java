/** 
 * Copyright (C) 2023 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze;

import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.handler.ArtifactAnalyzerHandler;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;

class DefaultArtifactAnalyzer implements ArtifactAnalyzer {

    private final List<ArtifactAnalyzerHandler> handlers;

    public DefaultArtifactAnalyzer(List<ArtifactAnalyzerHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public DependencyReport analyze(List<Artifact> artifacts) {
        DependencyReport dependencyReport = new DependencyReport();
        artifacts.forEach(artifact -> {
            try {
                analyze(artifact, dependencyReport, handlers);
            } catch (IOException e) {
                throw new AnalysisResultReportException("Failed to analyze artifacts: " + artifact.getId(), e);
            }
        });
        return dependencyReport;
    }

    private DependencyReport analyze(Artifact artifact, DependencyReport result, List<ArtifactAnalyzerHandler> handlers)
            throws IOException {
        for (var handler : handlers) {
            if (handler.appliesTo(artifact)) {
                handler.analyze(artifact, result);
            }
        }
        return result;
    }

}
