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
package org.bonitasoft.plugin.build.bar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.bonitasoft.bonita2bar.BarBuilder;
import org.bonitasoft.bonita2bar.BarBuilderFactory;
import org.bonitasoft.bonita2bar.BarBuilderFactory.BuildConfig;
import org.bonitasoft.bonita2bar.BuildBarException;
import org.bonitasoft.bonita2bar.ClasspathResolver;
import org.bonitasoft.bonita2bar.ConnectorImplementationRegistry;
import org.bonitasoft.bonita2bar.ConnectorImplementationRegistry.ConnectorImplementationJar;
import org.bonitasoft.bonita2bar.ProcessRegistry;
import org.bonitasoft.bonita2bar.SourcePathProvider;
import org.bonitasoft.bonita2bar.form.FormBuilder;
import org.bonitasoft.bpm.model.process.util.migration.MigrationPolicy;
import org.bonitasoft.plugin.AbstractBuildMojo;
import org.bonitasoft.plugin.analyze.report.DependencyReporter;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.build.page.BuildPageException;
import org.bonitasoft.plugin.build.page.UidArtifactBuilderFactory;
import org.bonitasoft.web.designer.config.UiDesignerProperties;

/**
 * This mojo builds Business archives from diagram sources.
 */
@Mojo(name = "business-archive", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Execute(goal = "analyze")
public class BuildBarMojo extends AbstractBuildMojo {

    private static final String[] DEFAULT_EXCLUDES = new String[0];
    private static final String[] DEFAULT_INCLUDES = new String[] { "**/*.proc" };

    /**
     * The configuration environment. Default to Local.
     */
    @Parameter(defaultValue = "local", property = "bonita.environment")
    String environment;

    /**
     * The name of the Bonita configuration file name. Default is ${project.artifactId}-${project.version}-${bonita.environment}.bconf
     */
    @Parameter(property = "bonita.configurationFile")
    String configurationFileName;

    /**
     * Whether task and process instantiation form mapping is required at build time
     * or not. Only Enterprise edition may update the form mapping at runtime.
     * Default to false
     */
    @Parameter(defaultValue = "false", property = "bonita.allowEmptyFormMapping")
    private boolean allowEmptyFormMapping;

    /**
     * Whether process parameter values are embedded in the Business archive file or not.
     * Only Enterprise edition may update the parameters values at runtime.
     * Default to true
     */
    @Parameter(defaultValue = "false", property = "bonita.includeParameters")
    private boolean includeParameters = false;

    /**
     * Whether process diagram files should try to migrate their content if needed
     * or not. Default to false
     */
    @Parameter(defaultValue = "false", property = "bonita.migrateIfNeeded")
    private boolean migrateIfNeeded;

    /**
     * Whether dependency jars should be included in the Business archive file. Default to true
     */
    @Parameter(defaultValue = "true", property = "bonita.includeDependencyJars")
    private boolean includeDependencyJars;

    /**
     * List of process diagram files to include.
     */
    @Parameter(property = "proc.includes")
    private String[] includes;

    /**
     * List of process diagram files to exclude.
     */
    @Parameter(property = "proc.excludes")
    private String[] excludes;

    /**
     * Maven ProjectHelper.
     */
    private MavenProjectHelper projectHelper;

    @Inject
    public BuildBarMojo(MavenProjectHelper projectHelper) {
        this.projectHelper = projectHelper;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path outputFolder = outputDirectory.toPath();

        var migrationPolicy = migrateIfNeeded ? MigrationPolicy.ALWAYS_MIGRATE_POLICY
                : MigrationPolicy.NEVER_MIGRATE_POLICY;
        var processRegistry = ProcessRegistry.of(selectedProcFiles(), migrationPolicy);
        var reportFile = outputDirectory.toPath().resolve("bonita-dependencies.json").toFile();
        if (!reportFile.exists()) {
            throw new MojoExecutionException("Dependency report is missing");
        }
        if (allowEmptyFormMapping) {
            getLog().warn(
                    "Empty form mapping is enabled. Processes without a form mapping will be unresolved after deployment.");
        }
        if (migrateIfNeeded) {
            getLog().warn(
                    "Process migration is enabled. If a process is in an older model version than expected, a migration will be attempted.");
        }
        var tmpFolder = outputFolder.resolve("business-archive-tmp");
        BarBuilder barBuilder;
        try {
            barBuilder = BarBuilderFactory.create(BuildConfig.builder()
                    .processRegistry(processRegistry)
                    .connectorImplementationRegistry(getConnectorImplementationRegistry(reportFile))
                    .allowEmptyFormMapping(allowEmptyFormMapping)
                    .includeParameters(includeParameters)
                    .sourcePathProvider(SourcePathProvider.of(project.getBasedir().toPath()))
                    .classpathResolver(ClasspathResolver.of(getClasspath()))
                    .formBuilder(createFormBuilder(uidWorkspaceProperties(outputFolder)))
                    .workingDirectory(tmpFolder)
                    .withDependencyJars(includeDependencyJars)
                    .build());
        } catch (BuildBarException e) {
            throw new MojoExecutionException(e);
        }

        for (var pool : processRegistry.getProcesses()) {
            try {
                var buildResult = barBuilder.build(pool, environment.toLowerCase());
                buildResult.writeBusinessArchivesTo(outputFolder.resolve("processes"));
                getLog().info("");
            } catch (BuildBarException | IOException e) {
                throw new MojoFailureException(
                        String.format("Failed to build %s (%s)", pool.getName(), pool.getVersion()), e);
            }
        }
        try {
            var aggregatedResult = barBuilder.getBuildResult();
            if (aggregatedResult != null && !aggregatedResult.getConfigurations().isEmpty()) {
                getLog().info("Building Bonita Configuration archive...");
                var bonitaConfigurationFile = outputDirectory.toPath().resolve(getConfigurationFileName(project));
                aggregatedResult.writeBonitaConfigurationTo(bonitaConfigurationFile);
                if (Files.exists(bonitaConfigurationFile)) {
                    projectHelper.attachArtifact(project, "bconf", environment.toLowerCase(),
                            bonitaConfigurationFile.toFile());
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }

    String getConfigurationFileName(MavenProject project) {
        if (configurationFileName == null) {
            return String.format("%s-%s-%s.bconf", project.getArtifactId(), project.getVersion(),
                    environment.toLowerCase());
        }
        return configurationFileName;
    }

    ConnectorImplementationRegistry getConnectorImplementationRegistry(File reportFile) throws MojoExecutionException {
        DependencyReport dependencyReport = new DependencyReport();
        try {
            dependencyReport = DependencyReporter.OBJECT_MAPPER.readValue(reportFile, DependencyReport.class);
        } catch (IOException e) {
            throw new MojoExecutionException("Dependency report is missing");
        }
        var implementations = new ArrayList<ConnectorImplementationJar>();
        dependencyReport.getConnectorImplementations().stream()
                .map(BuildBarMojo::toConnectorImplementationJar)
                .forEach(implementations::add);
        dependencyReport.getFilterImplementations().stream()
                .map(BuildBarMojo::toConnectorImplementationJar)
                .forEach(implementations::add);
        return ConnectorImplementationRegistry.of(implementations);
    }

    private static ConnectorImplementationJar toConnectorImplementationJar(Implementation implementation) {
        return ConnectorImplementationJar.of(implementation.getImplementationId(),
                implementation.getImplementationVersion(), new File(implementation.getArtifact().getFile()),
                implementation.getJarEntry());
    }

    private List<Path> selectedProcFiles() {
        var fileSetManager = new FileSetManager();
        var procFileSet = new FileSet();
        procFileSet.setDirectory(project.getBasedir().toPath().toString());
        procFileSet.setIncludes(Arrays.asList(getIncludes()));
        procFileSet.setExcludes(Arrays.asList(getExcludes()));
        return Stream.of(fileSetManager.getIncludedFiles(procFileSet))
                .map(procFile -> project.getBasedir().toPath().resolve(procFile)).collect(Collectors.toList());
    }

    private List<String> getClasspath() throws MojoExecutionException {
        try {
            return Stream.concat(project.getCompileClasspathElements().stream(),
                    project.getRuntimeClasspathElements().stream()).distinct().collect(Collectors.toList());
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e);
        }
    }

    FormBuilder createFormBuilder(UiDesignerProperties uiDesignerProperties) {
        var artifactBuilder = UidArtifactBuilderFactory.create(uiDesignerProperties);
        return new FormBuilder() {

            @Override
            public byte[] export(String formId) throws IOException {
                try {
                    return artifactBuilder.buildPage(formId);
                } catch (BuildPageException e) {
                    throw new IOException(String.format("Failed to build form %s", formId), e);
                }
            }
        };
    }

    private String[] getIncludes() {
        if (includes != null && includes.length > 0) {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes() {
        if (excludes != null && excludes.length > 0) {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }

}
