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
package org.bonitasoft.plugin.analyze.handler;

import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.bonitasoft.plugin.analyze.ConnectorResolver;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.eclipse.aether.repository.LocalRepositoryManager;

public interface ArtifactAnalyzerHandler {

    boolean appliesTo(Artifact artifact);

    DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException;

    public static List<ArtifactAnalyzerHandler> create(ConnectorResolver connectorResolver,
            LocalRepositoryManager localRepositoryManager,
            MavenResourcesFiltering mavenResourcesFiltering,
            List<MavenProject> reactorProjects) {
        return List.of(new ConnectorAnalyzer(localRepositoryManager, connectorResolver),
                new CustomPageAnalyzer(localRepositoryManager),
                new CustomPageProjectAnalyzer(localRepositoryManager, mavenResourcesFiltering, reactorProjects),
                new ApplicationDescriptorAnalyzer(localRepositoryManager));
    }

}
