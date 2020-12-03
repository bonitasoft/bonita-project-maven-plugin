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
package org.bonitasoft.plugin;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

@Mojo(name = "install", defaultPhase = LifecyclePhase.VALIDATE)
public class InstallProjectStoreMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    /**
     * Project store where dependencies are stored
     */
    @Parameter(defaultValue = ".store", required = true)
    private File projectStore;

    @Component
    private Maven maven;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    @Component
    private ArtifactResolver artifactResolver;

    /**
     * Remote repositories which will be searched for artifacts.
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> remoteRepositories;

    /**
     * Local Repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    protected ArtifactRepository localRepository;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Artifact> artifacts = project.getDependencyArtifacts();
        for (Artifact artifact : artifacts) {
            File artifactFile = findFileInProjectStore(artifact);
            if (artifactFile.exists() && artifact.isSnapshot()) { // Always update SNAPSHOT dependencies
                try {
                    installArtifact(artifact, artifactFile);
                } catch (InstallFileExecutionException e) {
                    throw new MojoExecutionException(e.getMessage());
                }
            }
            ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
            try {
                artifactResolver.resolveArtifact(buildingRequest, artifact);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException(String.format("Failed to resolve artifact %s", artifact), e);
            } catch (ArtifactResolverException e) {
                try {
                    installArtifactFromProjectStore(artifact);
                } catch (InstallFileExecutionException | ArtifactNotFoundException ex) {
                    throw new MojoExecutionException(ex.getMessage());
                }
            }
        }
    }

    private void installArtifactFromProjectStore(Artifact artifact)
            throws ArtifactNotFoundException, InstallFileExecutionException {
        File artifactFile = findFileInProjectStore(artifact);
        if (!artifactFile.exists()) {
            throw new ArtifactNotFoundException(
                    String.format("Failed to install artifact %s. Artifact file Not found: %s", artifact, artifactFile),
                    artifact);
        }
        installArtifact(artifact, artifactFile);
    }

    private void installArtifact(Artifact artifact, File artifactFile) throws InstallFileExecutionException {
        MavenExecutionResult executionResult = maven.execute(newInstallFileExecutionRequest(artifact, artifactFile));
        if (executionResult.hasExceptions()) {
            throw new InstallFileExecutionException(executionResult.getExceptions());
        }
    }

    private File findFileInProjectStore(Artifact artifact) {
        String artifactPath = String.format("%s/%s/%s/%s",
                artifact.getGroupId().replace(".", "/"),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifactFileName(artifact));
        return projectStore.toPath().resolve(Paths.get(artifactPath)).toFile();
    }

    private String artifactFileName(Artifact artifact) {
        if (artifact.getClassifier() != null) {
            return String.format("%s-%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(),
                    artifact.getClassifier(), artifact.getType());
        }
        return String.format("%s-%s.%s", artifact.getArtifactId(), artifact.getVersion(), artifact.getType());
    }

    private MavenExecutionRequest newInstallFileExecutionRequest(Artifact artifact, File artifactFile) {
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setGoals(Arrays.asList("install:install-file"));
        executionRequest.setLocalRepository(localRepository);
        Properties installFileProperties = new Properties();
        installFileProperties.setProperty("groupId", artifact.getGroupId());
        installFileProperties.setProperty("artifactId", artifact.getArtifactId());
        installFileProperties.setProperty("version", artifact.getVersion());
        installFileProperties.setProperty("file", artifactFile.getAbsolutePath());
        if (artifact.getClassifier() != null) {
            installFileProperties.setProperty("classifier", artifact.getClassifier());
        }
        installFileProperties.setProperty("packaging", artifact.getType());
        executionRequest.setUserProperties(installFileProperties);
        return executionRequest;
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
