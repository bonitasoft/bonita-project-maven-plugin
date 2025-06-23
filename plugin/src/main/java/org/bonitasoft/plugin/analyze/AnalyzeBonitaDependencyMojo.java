/** 
 * Copyright (C) 2020 BonitaSoft S.A.
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

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.bonitasoft.plugin.MavenSessionExecutor;
import org.bonitasoft.plugin.MavenSessionExecutor.BuildException;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.DependencyReporter;
import org.bonitasoft.plugin.analyze.report.JsonDependencyReporter;
import org.bonitasoft.plugin.analyze.report.LogDependencyReporter;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.codehaus.plexus.util.StringUtils;

/**
 * This mojo runs an analysis on the current project dependencies to detect
 * Bonita specific extensions.
 * <p>Note: extensions in reactor must be compiled first, so we can inspect the class hierarchy.</p>
 */
@Mojo(name = "analyze", aggregator = true)
public class AnalyzeBonitaDependencyMojo extends AbstractMojo {

    protected final ArtifactResolver artifactResolver;

    protected final ArtifactAnalyzerFactory artifactAnalyzerFactory;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", required = true, readonly = true)
    protected List<MavenProject> reactorProjects;

    /**
     * Remote repositories which will be searched for artifacts.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * Analysis report output file.
     */
    @Parameter(defaultValue = "${project.build.directory}/bonita-dependencies.json", property = "bonita.analyze.outputFile")
    protected File outputFile;

    /**
     * Look for incompatible dependencies.
     */
    @Parameter(defaultValue = "true", property = "bonita.validateDependencies")
    protected boolean validateDeps;

    private DependencyValidator dependencyValidator;

    private ProjectArtifactFactory artifactFactory;

    /**
     * Scope threshold to include. An empty string indicates include all
     * dependencies. Default value is runtime.<br>
     * The scope threshold value being interpreted is the scope as Maven filters for
     * creating a classpath, not as specified in the pom. In summary:
     * <ul>
     * <li><code>runtime</code> include scope gives runtime and compile
     * dependencies,</li>
     * <li><code>compile</code> include scope gives compile, provided, and system
     * dependencies,</li>
     * <li><code>test</code> include scope gives all dependencies (equivalent to
     * default),</li>
     * <li><code>provided</code> include scope just gives provided
     * dependencies,</li>
     * <li><code>system</code> include scope just gives system dependencies.</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter(property = "includeScope", defaultValue = "runtime")
    protected String includeScope = "runtime";

    /**
     * Scope threshold to exclude, if no value is defined for include. An empty
     * string indicates no dependencies (default).<br>
     * The scope threshold value being interpreted is the scope as Maven filters for
     * creating a classpath, not as specified in the pom. In summary:
     * <ul>
     * <li><code>runtime</code> exclude scope excludes runtime and compile
     * dependencies,</li>
     * <li><code>compile</code> exclude scope excludes compile, provided, and system
     * dependencies,</li>
     * <li><code>test</code> exclude scope excludes all dependencies, then not
     * really a legitimate option: it will fail, you probably meant to configure
     * includeScope = compile</li>
     * <li><code>provided</code> exclude scope just excludes provided
     * dependencies,</li>
     * <li><code>system</code> exclude scope just excludes system dependencies.</li>
     * </ul>
     *
     * @since 0.1.0
     */
    @Parameter(property = "excludeScope", defaultValue = "")
    protected String excludeScope;

    private List<BuildException> compilationErrors = new ArrayList<>();

    @Inject
    public AnalyzeBonitaDependencyMojo(ArtifactResolver artifactResolver,
            ArtifactAnalyzerFactory artifactAnalyzerFactory,
            DependencyValidator dependencyValidator, ProjectArtifactFactory artifactFactory) {
        this.artifactResolver = artifactResolver;
        this.artifactAnalyzerFactory = artifactAnalyzerFactory;
        this.dependencyValidator = dependencyValidator;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var appModuleProject = findAppModuleProject();

        ProjectBuildingRequest buildingRequest = newProjectBuildingRequest(appModuleProject);
        List<Artifact> resolvedArtifacts;
        try {
            resolvedArtifacts = resolveArtifacts(getProjectArtifacts(appModuleProject), buildingRequest);
        } catch (MojoExecutionException | ArtifactFilterException e) {
            throw new MojoExecutionException(e);
        }
        var artifactAnalyzer = artifactAnalyzerFactory
                .create(session.getRepositorySession().getLocalRepositoryManager(), reactorProjects);
        DependencyReport dependencyReport = artifactAnalyzer.analyze(resolvedArtifacts);
        compilationErrors.forEach(error -> dependencyReport.addIssue(
                Issue.create(Issue.Type.EXTENSION_COMPILATION_ERROR, error.getMessage(), Issue.Severity.ERROR,
                        ExceptionUtils.getStackTrace(error))));

        if (validateDeps) {
            dependencyValidator.validate(project, buildingRequest).stream().forEach(dependencyReport::addIssue);
        }

        if (outputFile != null) {
            var buildFolder = appModuleProject.getBuild().getDirectory();
            outputFile = Paths.get(buildFolder).resolve(outputFile.getName()).toFile();
        }
        getReporters().forEach(reporter -> reporter.report(dependencyReport));
    }

    MavenProject findAppModuleProject() throws MojoExecutionException {
        return reactorProjects.size() == 1 ? project
                : reactorProjects.stream().filter(p -> p.getBasedir().getName().equals("app")).findFirst().orElseThrow(
                        () -> new MojoExecutionException(String.format("Application module not found in %s",
                                project.getBasedir().toPath().resolve("app"))));
    }

    private Set<Artifact> getProjectArtifacts(MavenProject appModuleProject) throws MojoExecutionException {
        try {
            return artifactFactory.createArtifacts(appModuleProject);
        } catch (InvalidDependencyVersionException e) {
            throw new MojoExecutionException(e);
        }
    }

    protected List<DependencyReporter> getReporters() {
        List<DependencyReporter> reporters = new ArrayList<>();
        reporters.add(new LogDependencyReporter(getLog()));
        if (outputFile != null) {
            reporters.add(new JsonDependencyReporter(outputFile));
        }
        return reporters;
    }

    protected List<Artifact> resolveArtifacts(Set<Artifact> artifacts, ProjectBuildingRequest buildingRequest)
            throws ArtifactFilterException {
        var filter = new FilterArtifacts();
        filter.addFilter(new ScopeFilter(cleanToBeTokenizedString(this.includeScope),
                cleanToBeTokenizedString(this.excludeScope)));
        return filter.filter(artifacts).stream().map(artifact -> resolve(buildingRequest, artifact)).collect(toList());
    }

    Artifact resolve(ProjectBuildingRequest buildingRequest, Artifact artifact) {
        try {
            ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, artifact);
            final Artifact resolvedArtifact = result.getArtifact();
            File artifactFile = resolvedArtifact.getFile();
            if (artifactFile == null || !artifactFile.exists()) {
                throw new MojoExecutionException(format("Failed to resolve artifact %s", artifact));
            }
            return resolvedArtifact;
        } catch (ArtifactResolverException are) {
            var moduleProject = reactorProjects.stream()
                    .filter(p -> matchesCoordinates(artifact, p))
                    .findFirst()
                    .orElseThrow(() -> new AnalysisResultReportException(
                            format("Failed to analyze artifact %s", artifact), are));
            // Handle child modules specific case
            // Artifact might not be installed in local repository yet
            artifact.setFile(moduleProject.getBasedir());
            compileExtensionModule(artifact);
            return artifact;
        } catch (Exception e) {
            throw new AnalysisResultReportException(format("Failed to analyze artifact %s", artifact), e);
        }
    }

    private void compileExtensionModule(Artifact artifact) {
        var artifactBaseDir = artifact.getFile();
        if (artifactBaseDir.isDirectory() && "jar".equals(Optional.ofNullable(artifact.getType()).orElse("jar"))) {
            var mavenProject = reactorProjects.stream()
                    .filter(p -> Objects.equals(artifactBaseDir, p.getBasedir()))
                    .findFirst();
            mavenProject.ifPresent(p -> {
                try {
                    MavenSessionExecutor.fromSession(session).execute(p.getModel().getPomFile(),
                            project.getBasedir(),
                            List.of("compiler:compile"),
                            Map.of(), List.of(),
                            () -> "Error while compiling extension module " + p.getArtifactId());
                } catch (BuildException e) {
                    // build failed, we do not want to fail the whole analysis, but only report the error
                    compilationErrors.add(e);
                }
            });
        }
    }

    private boolean matchesCoordinates(Artifact artifact, MavenProject p) {
        Artifact projectArtifact = p.getArtifact();
        return Objects.equals(projectArtifact.getGroupId(), artifact.getGroupId())
                && Objects.equals(projectArtifact.getArtifactId(), artifact.getArtifactId())
                && Objects.equals(projectArtifact.getBaseVersion(), artifact.getBaseVersion());
    }

    /*
     * @return Returns a new ProjectBuildingRequest populated from the current
     * session and the current project remote repositories, used to resolve
     * artifacts.
     */
    ProjectBuildingRequest newProjectBuildingRequest(MavenProject appModuleProject) {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(remoteRepositories);
        buildingRequest.setProject(appModuleProject);
        return buildingRequest;
    }

    /**
     * Clean up configuration string before it can be tokenized.
     * 
     * @param str the string which should be cleaned
     * @return cleaned up string
     */
    static String cleanToBeTokenizedString(String str) {
        String ret = "";
        if (!StringUtils.isEmpty(str)) {
            // remove initial and ending spaces, plus all spaces next to commas
            ret = str.trim().replaceAll("[\\s]*,[\\s]*", ",");
        }

        return ret;
    }

}
