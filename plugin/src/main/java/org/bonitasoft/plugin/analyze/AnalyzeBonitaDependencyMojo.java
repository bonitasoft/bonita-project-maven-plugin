/**
 * Copyright (C) 2020 BonitaSoft S.A.
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
package org.bonitasoft.plugin.analyze;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.DependencyReporter;
import org.bonitasoft.plugin.analyze.report.JsonDependencyReporter;
import org.bonitasoft.plugin.analyze.report.LogDependencyReporter;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;

@Mojo(name = "analyze", defaultPhase = LifecyclePhase.NONE)
public class AnalyzeBonitaDependencyMojo extends AbstractMojo {

    protected final ArtifactResolver artifactResolver;

    protected final ArtifactAnalyser artifactAnalyser;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * Local Repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Remote repositories which will be searched for artifacts.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepositories;

    /**
     * Analysis report output file
     */
    @Parameter(defaultValue = "${project.build.directory}/bonita-dependencies.json")
    protected File outputFile;

    /**
     * Look for incompatible dependencies
     */
    @Parameter(defaultValue = "true", property = "bonita.validateDependencies")
    protected boolean validateDeps;

    private DependencyValidator dependencyValidator;

    private ProjectArtifactFactory artifactFactory;

    @Inject
    public AnalyzeBonitaDependencyMojo(ArtifactResolver artifactResolver,
            ArtifactAnalyser artifactAnalyser,
            DependencyValidator dependencyValidator,
            ProjectArtifactFactory artifactFactory) {
        this.artifactResolver = artifactResolver;
        this.artifactAnalyser = artifactAnalyser;
        this.dependencyValidator = dependencyValidator;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProjectBuildingRequest buildingRequest = newProjectBuildingRequest();
        List<Artifact> resolvedArtifacts = resolveArtifacts(getProjectArtifacts(), buildingRequest);
        DependencyReport dependencyReport = artifactAnalyser.analyse(resolvedArtifacts);

        if (validateDeps) {
            dependencyValidator.validate(project, buildingRequest).stream()
                    .forEach(dependencyReport::addIssue);
        }

        getReporters().forEach(reporter -> reporter.report(dependencyReport));
    }

    private Set<Artifact> getProjectArtifacts() throws MojoExecutionException {
        try {
           return artifactFactory.createArtifacts(project);
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

    protected List<Artifact> resolveArtifacts(Set<Artifact> artifacts, ProjectBuildingRequest buildingRequest) {
        return artifacts.stream()
                .filter(artifact -> Objects.equals(artifact.getScope(), Artifact.SCOPE_COMPILE))
                .map(artifact -> {
            try {
                ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, artifact);
                final Artifact resolvedArtifact = result.getArtifact();
                File artifactFile = resolvedArtifact.getFile();
                if (artifactFile == null || !artifactFile.exists()) {
                    throw new MojoExecutionException(format("Failed to resolve artifact %s", artifact));
                }
                return resolvedArtifact;
            } catch (Exception e) {
                throw new AnalysisResultReportException(format("Failed to analyze artifact %s", artifact), e);
            }
        }).collect(toList());
    }

    /*
     * @return Returns a new ProjectBuildingRequest populated from the current session and the current project remote
     * repositories, used to resolve artifacts.
     */
    ProjectBuildingRequest newProjectBuildingRequest() {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setRemoteRepositories(remoteRepositories);
        buildingRequest.setProject(project);
        return buildingRequest;
    }

}
