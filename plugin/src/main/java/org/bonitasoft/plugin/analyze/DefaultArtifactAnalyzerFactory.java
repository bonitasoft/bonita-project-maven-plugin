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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.bonitasoft.plugin.analyze.handler.ArtifactAnalyzerHandler;
import org.eclipse.aether.repository.LocalRepositoryManager;

@Named
@Singleton
class DefaultArtifactAnalyzerFactory implements ArtifactAnalyzerFactory {

    private ConnectorResolver connectorResolver;
    private MavenResourcesFiltering mavenResourcesFiltering;

    @Inject
    public DefaultArtifactAnalyzerFactory(ConnectorResolver connectorResolver,
            MavenResourcesFiltering mavenResourcesFiltering) {
        this.connectorResolver = connectorResolver;
        this.mavenResourcesFiltering = mavenResourcesFiltering;
    }

    @Override
    public ArtifactAnalyzer create(LocalRepositoryManager localRepositoryManager, List<MavenProject> reactorProjects) {
        return new DefaultArtifactAnalyzer(ArtifactAnalyzerHandler.create(connectorResolver, localRepositoryManager,
                mavenResourcesFiltering, reactorProjects));
    }

}
