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
package org.bonitasoft.plugin.install;

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
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.version.DefaultPluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
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

    private static final String INSTALL_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String INSTALL_PLUGIN_ARTIFACT_ID = "maven-install-plugin";

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
    
    @Component
    private PluginVersionResolver pluginVersionResolver;

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
        String installPluginVersion = "2.5.2";
        try {
             installPluginVersion = resolveInstallFilePluginVersion();
        } catch (PluginVersionResolutionException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        for (Artifact artifact : artifacts) {
            File artifactFile = findFileInProjectStore(artifact);
            if (artifactFile.exists() && artifact.isSnapshot()) { // Always update SNAPSHOT dependencies
                try {
                    installArtifact(artifact, artifactFile, installPluginVersion);
                } catch (InstallFileExecutionException  e) {
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
                    installArtifactFromProjectStore(artifact, installPluginVersion);
                } catch (InstallFileExecutionException | ArtifactNotFoundException ex) {
                    throw new MojoExecutionException(ex.getMessage());
                }
            }
        }
    }

    private void installArtifactFromProjectStore(Artifact artifact, String installPluginVersion)
            throws ArtifactNotFoundException, InstallFileExecutionException {
        File artifactFile = findFileInProjectStore(artifact);
        if (!artifactFile.exists()) {
            throw new ArtifactNotFoundException(
                    String.format("Failed to install artifact %s. Artifact file Not found: %s", artifact, artifactFile),
                    artifact);
        }
        installArtifact(artifact, artifactFile, installPluginVersion);
    }

    private void installArtifact(Artifact artifact, File artifactFile, String installPluginVersion) throws InstallFileExecutionException {
        MavenExecutionResult executionResult = maven.execute(newInstallFileExecutionRequest(artifact, artifactFile, installPluginVersion));
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

    private MavenExecutionRequest newInstallFileExecutionRequest(Artifact artifact, File artifactFile, String installPluginVersion) {
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        executionRequest.setGoals(Arrays.asList(String.format("%s:%s:%s:install-file",
                INSTALL_PLUGIN_GROUP_ID,
                INSTALL_PLUGIN_ARTIFACT_ID,
                installPluginVersion)));
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

    private String resolveInstallFilePluginVersion() throws PluginVersionResolutionException {
        Plugin plugin = new Plugin();
        plugin.setGroupId(INSTALL_PLUGIN_GROUP_ID);
        plugin.setArtifactId(INSTALL_PLUGIN_ARTIFACT_ID);
        DefaultPluginVersionRequest pluginVersionRequest = new DefaultPluginVersionRequest(plugin, session);
        pluginVersionRequest.setPom(project.getModel());
        PluginVersionResult pluginVersionResult = pluginVersionResolver.resolve(pluginVersionRequest);
        return pluginVersionResult.getVersion();
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
