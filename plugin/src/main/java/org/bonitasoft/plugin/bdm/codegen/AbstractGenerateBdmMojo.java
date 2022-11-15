/**
 * Copyright (C) 2022 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.bdm.codegen;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.function.Predicate;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.sonatype.plexus.build.incremental.BuildContext;

public abstract class AbstractGenerateBdmMojo extends AbstractMojo {

    /**
     * The path to a Business Object Model descriptor file.
     * 
     * @since 0.1.0
     */
    @Parameter(required = true)
    protected File bdmModelFile;

    /**
     * The path to the output folder where the source code is generated.
     * 
     * @since 0.1.0
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/java")
    protected File outputFolder;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    private BusinessDataModelParser businessDataModelReader;
    private BusinessDataModelGenerator generator;
    private BuildContext buildContext;

    AbstractGenerateBdmMojo(BusinessDataModelParser businessDataModelReader,
            BusinessDataModelGenerator generator,
            BuildContext buildContext) {
        this.businessDataModelReader = businessDataModelReader;
        this.generator = generator;
        this.buildContext = buildContext;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (bdmModelFile == null || !bdmModelFile.exists()) {
            getLog().info("Skipping Business Data Model sources generation (no descriptor found)...");
            return;
        }
        
        if (!buildContext.hasDelta(bdmModelFile)) {
            getLog().info("Skipping Business Data Model sources generation (generated sources are up to date)...");
            return;
        }
        
        getLog().info("Generating Business Data Model sources...");

        var instant = Instant.now();
        BusinessObjectModel model = null;
        try {
            model = businessDataModelReader.parse(bdmModelFile);
        } catch (ParseException e) {
            throw new MojoFailureException("Error while parsing the model descriptor", e);
        }

        if (outputFolder.exists()) { // Avoid duplicates in output folder
            try (var files = Files.walk(outputFolder.toPath())) { // Delete outputFolder content
                files.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new MojoFailureException(
                        String.format("Failed to clean output directory: %s", outputFolder));
            }
        }
        if (!outputFolder.mkdirs()) {
            throw new MojoFailureException(
                    String.format("Failed to create output directory: %s", outputFolder));
        }

        try {
            generator.generate(model, outputFolder.toPath());
        } catch (CodeGenerationException e) {
            throw new MojoFailureException("Error while generating bdm model sources", e);
        }

        try (var files = Files.walk(outputFolder.toPath())) {
            files.filter(exludedGeneratedSources())
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }

        getLog().info(String.format("Business Data Model model sources generation completed in %s.",
                Duration.between(Instant.now(), instant)));
        
        buildContext.refresh(outputFolder);
    }

    protected abstract Predicate<Path> exludedGeneratedSources();

}
