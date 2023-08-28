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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.bonitasoft.bonita2bar.configuration.ParameterConfigurationExtractor;

/**
 * This mojo extracts parameters from a Bonita configuration archive.
 */
@Mojo(name = "extract-configuration", aggregator = true, requiresProject = true)
public class ExtractConfigurationArchiveMojo extends AbstractConfigurationArchiveMojo {

    protected ParameterConfigurationExtractor extractor = new ParameterConfigurationExtractor();

    /**
     * Extract parameters without their values. Default is false.
     */
    @Parameter(property = "withoutParametersValue", defaultValue = "false")
    protected boolean withoutParametersValue;

    /**
     * The name of output file extracted from the Bonita configuration file. Default is parameters-${bonita.environment}.yml
     */
    @Parameter(property = "outputFileName", defaultValue = "parameters-${bonita.environment}.yml")
    protected String outputFileName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var confFile = bonitaConfiguration != null ? new File(bonitaConfiguration)
                : defaultConfigurationFile();
        if (!confFile.exists()) {
            throw new MojoExecutionException(
                    String.format("%s Bonita configuration archive does not exists.", confFile.getAbsolutePath()));
        }
        try {
            extractor.extract(confFile,
                    new File(getAppModuleBuildDir(), outputFileName()).getAbsolutePath(),
                    withoutParametersValue);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    private String outputFileName() throws MojoExecutionException {
        if (outputFileName.contains("${bonita.environment}")) {
            outputFileName = outputFileName.replace("${bonita.environment}", getEnvironment());
        }
        return outputFileName;
    }

}
