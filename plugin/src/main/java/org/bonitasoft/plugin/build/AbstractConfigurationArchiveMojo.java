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
package org.bonitasoft.plugin.build;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractConfigurationArchiveMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", required = true, readonly = true)
    protected List<MavenProject> reactorProjects;

    /**
     * The Bonita environment.
     */
    @Parameter(property = "bonita.environment")
    protected String environment;

    /**
     * Parameters file
     */
    @Parameter(property = "bonita.parametersFile", defaultValue = "${maven.multiModuleProjectDirectory}/.bcd_configurations/parameters-${bonita.environment}.yml")
    protected String parametersFile;

    protected MavenProject findAppModuleProject() throws MojoExecutionException {
        return reactorProjects.size() == 1 ? project
                : reactorProjects.stream()
                        .filter(p -> p.getBasedir().getName().equals("app"))
                        .findFirst()
                        .orElseThrow(
                                () -> new MojoExecutionException(String.format("Application module not found in %s",
                                        project.getBasedir().toPath().resolve("app"))));
    }

    protected File defaultConfigurationFile() throws MojoExecutionException {
        var appModule = findAppModuleProject();
        return appModule.getAttachedArtifacts().stream()
                .filter(artifact -> "bconf".equals(artifact.getType()))
                .map(Artifact::getFile)
                .findFirst()
                .orElse(null);
    }

    protected String getEnvironment() throws MojoExecutionException {
        if (environment == null) {
            var appModule = findAppModuleProject();
            environment = appModule.getProperties().getProperty("bonita.environment");
            if (environment == null) {
                throw new MojoExecutionException("Required bonita.environment properties is not set.");
            }
        }
        return environment.toLowerCase();
    }

    protected File getAppModuleBuildDir() throws MojoExecutionException {
        var appModule = findAppModuleProject();
        return new File(appModule.getBuild().getDirectory());
    }

    protected File getAppModuleBaseDir() throws MojoExecutionException {
        var appModule = findAppModuleProject();
        return appModule.getBasedir();
    }
}
