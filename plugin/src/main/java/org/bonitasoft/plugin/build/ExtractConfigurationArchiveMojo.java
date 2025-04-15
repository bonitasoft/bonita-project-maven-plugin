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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.bonitasoft.bonita2bar.BarBuilder;
import org.bonitasoft.bonita2bar.BarBuilderFactory;
import org.bonitasoft.bonita2bar.BarBuilderFactory.BuildConfig;
import org.bonitasoft.bonita2bar.BuildBarException;
import org.bonitasoft.bonita2bar.ConnectorImplementationRegistry;
import org.bonitasoft.bonita2bar.ProcessRegistry;
import org.bonitasoft.bonita2bar.configuration.ParameterConfigurationExtractor;
import org.bonitasoft.bpm.model.process.util.migration.MigrationPolicy;
import org.bonitasoft.plugin.MavenSessionExecutor;

/**
 * <p>This mojo extracts parameters from all the processes found in the project into a single parameters file.</p>
 * <p>This does <b>not</b> extract parameters from a Bonita configuration archive you may have updated.</p>
 */
@Mojo(name = "extract-configuration", aggregator = true, requiresProject = true)
public class ExtractConfigurationArchiveMojo extends AbstractConfigurationArchiveMojo {

    protected ParameterConfigurationExtractor extractor = new ParameterConfigurationExtractor();

    /**
     * Only extract parameters without values. Default is false.
     */
    @Parameter(property = "parameters.withoutValue", defaultValue = "false")
    protected boolean withoutParametersValue;

    /**
     * Overwrite existing parameters file. Default is false.
     */
    @Parameter(property = "parameters.overwrite", defaultValue = "false")
    protected boolean overwrite;

    /**
     * Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var tmpFolder = getTempFolder();
        var barBuilder = createBarBuilder(tmpFolder);
        var confFile = tmpFolder.resolve("configuration.bconf");
        try {
            var result = barBuilder.buildConfiguration(environment.toLowerCase());
            result.writeBonitaConfigurationTo(confFile);
            if (!Files.exists(confFile)) {
                return;
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to extract process configurations.", e);
        }
        var outputFile = new File(parametersFile);
        if (outputFile.exists() && !overwrite) {
            throw new MojoFailureException(String.format(
                    "%s already exists. Overwrite the existing parameters file setting parameters.overwrite property to true. ",
                    outputFile));
        }
        try {
            Files.createDirectories(outputFile.toPath().getParent());
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
        try {
            extractor.extract(confFile.toFile(),
                    outputFile.getAbsolutePath(),
                    withoutParametersValue);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        } finally {
            try {
                Files.deleteIfExists(confFile);
            } catch (IOException e) {
                getLog().debug("An error occured while deleting {}", e);
            }
        }
    }

    Path getTempFolder() throws MojoExecutionException {
        return getAppModuleBuildDir().toPath().resolve("extract-configuration-tmp");
    }

    BarBuilder createBarBuilder(Path tmpFolder) throws MojoExecutionException {
        var processRegistry = ProcessRegistry.of(selectedProcFiles(), MigrationPolicy.ALWAYS_MIGRATE_POLICY);
        try {
            return BarBuilderFactory.create(BuildConfig.builder()
                    .processRegistry(processRegistry)
                    .connectorImplementationRegistry(ConnectorImplementationRegistry.of(List.of()))
                    .allowEmptyFormMapping(true)
                    .includeParameters(false)
                    .mavenProject(findAppModuleProject())
                    .mavenExecutor(MavenSessionExecutor.fromSession(session))
                    .formBuilder(id -> new byte[0])
                    .workingDirectory(tmpFolder)
                    .build());
        } catch (BuildBarException e) {
            throw new MojoExecutionException(e);
        }
    }

    private List<Path> selectedProcFiles() throws MojoExecutionException {
        var fileSetManager = new FileSetManager();
        var procFileSet = new FileSet();
        var appModuleBaesir = getAppModuleBaseDir();
        procFileSet.setDirectory(appModuleBaesir.toPath().toString());
        procFileSet.setIncludes(List.of("**/*.proc"));
        return Stream.of(fileSetManager.getIncludedFiles(procFileSet))
                .map(procFile -> appModuleBaesir.toPath().resolve(procFile)).collect(Collectors.toList());
    }

}
