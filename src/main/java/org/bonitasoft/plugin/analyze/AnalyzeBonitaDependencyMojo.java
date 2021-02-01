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

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReporter;
import org.bonitasoft.plugin.analyze.report.CsvAnalysisResultReporter;
import org.bonitasoft.plugin.analyze.report.LogAnalysisResultReporter;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

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
	@Parameter(defaultValue = "${project.build.directory}/bonita-dependencies.csv", required = true)
	protected File outputFile;

	@Inject
	public AnalyzeBonitaDependencyMojo(ArtifactResolver artifactResolver, ArtifactAnalyser artifactAnalyser) {
		this.artifactResolver = artifactResolver;
		this.artifactAnalyser = artifactAnalyser;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<Artifact> resolvedArtifacts = resolveArtifacts(project.getDependencyArtifacts());
		AnalysisResult analysisResult = artifactAnalyser.analyse(resolvedArtifacts);
		getReporters().forEach(reporter -> reporter.report(analysisResult));
	}

	protected List<AnalysisResultReporter> getReporters() {
		return asList(
				new LogAnalysisResultReporter(getLog()),
				new CsvAnalysisResultReporter(outputFile)
		);
	}

	protected List<Artifact> resolveArtifacts(Set<Artifact> artifacts) {
		return artifacts.stream().map(artifact -> {
			ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
			try {
				ArtifactResult result = artifactResolver.resolveArtifact(buildingRequest, artifact);
				final Artifact resolvedArtifact = result.getArtifact();
				File artifactFile = resolvedArtifact.getFile();
				if (artifactFile == null || !artifactFile.exists()) {
					throw new MojoExecutionException(format("Failed to resolve artifact %s", artifact));
				}
				return resolvedArtifact;
			}
			catch (Exception e) {
				throw new AnalysisResultReportException(format("Failed to analyse artifact %s", artifact), e);
			}
		}).collect(toList());
	}

	/*
	 * @return Returns a new ProjectBuildingRequest populated from the current session and the current project remote
	 * repositories, used to resolve artifacts.
	 */
	private ProjectBuildingRequest newResolveArtifactProjectBuildingRequest() {
		ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
		buildingRequest.setRemoteRepositories(remoteRepositories);
		return buildingRequest;
	}

}
