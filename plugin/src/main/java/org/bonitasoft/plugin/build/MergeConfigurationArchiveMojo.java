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
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bonitasoft.bonita2bar.configuration.ParametersConfigurationMerger;

/**
 * This mojo merges given parameters into a Bonita configuration archive.
 */
@Mojo(name = "merge-configuration", defaultPhase = LifecyclePhase.PACKAGE, aggregator = true, requiresProject = true)
public class MergeConfigurationArchiveMojo extends AbstractConfigurationArchiveMojo {

    protected ParametersConfigurationMerger merger = new ParametersConfigurationMerger();

    /**
     * Parameters file to merge
     */
    @Parameter(property = "bonita.parametersFile", defaultValue = "${maven.multiModuleProjectDirectory}/.bcd_configurations/parameters-${bonita.environment}.yml")
    protected String parametersFile;

    /**
     * Skip execution
     */
    @Parameter(property = "bonita.skipMergeParameters", defaultValue = "false")
    protected boolean skipMergeParameters = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipMergeParameters) {
            getLog().info("Skipped.");
            return;
        }
        var input = defaultInputFile();
        if (!input.exists()) {
            getLog().info(String.format("Skipped. %s parameters file not found.", input.getAbsolutePath()));
            return;
        }
        var confFile = bonitaConfiguration != null ? new File(bonitaConfiguration)
                : defaultConfigurationFile();
        if (!confFile.exists()) {
            throw new MojoExecutionException(
                    String.format("%s Bonita configuration archive does not exists.", confFile.getAbsolutePath()));
        }
        try {
            merger.merge(confFile,
                    input,
                    confFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private File defaultInputFile() throws MojoExecutionException {
        if (parametersFile.contains("${bonita.environment}")) {
            parametersFile = parametersFile.replace("${bonita.environment}", getEnvironment());
        }
        return new File(parametersFile);
    }

}
