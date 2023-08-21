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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * This mojo generates an application.properties with project information.
 */
@Mojo(name = "application-properties", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
public class ApplicationPropertiesMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var properties = new Properties();
        properties.setProperty("version", project.getVersion());
        try {
            writeApplicationProperties(properties, getOuputDir());
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    void writeApplicationProperties(Properties properties, Path outputDir) throws IOException {
        var outputFile = outputDir.resolve("application.properties");
        try (var os = Files.newOutputStream(outputFile, StandardOpenOption.CREATE)) {
            properties.store(os, null);
            getLog().info(String.format("application.properties written in %s", outputDir));
        }
    }

    Path getOuputDir() throws IOException {
        var outputDir = new File(project.getBuild().getDirectory()).toPath();
        Files.createDirectories(outputDir);
        return outputDir;
    }

}
