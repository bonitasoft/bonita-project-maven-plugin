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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.inject.Inject;

import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
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
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;

/**
 * This mojo looks for unknown dependencies in the current project 
 * and look for them in a project local dependency store (.store folder by default).
 * Install missing dependencies found in the local store in the local repository.
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.NONE)
public class InstallProjectStoreMojo extends AbstractMojo {

    private static final String GROUP_ID = "groupId";
    private static final String VERSION = "version";
    private static final String ARTIFACT_ID = "artifactId";

    static final String DEFAULT_INSTALL_PLUGIN_VERSION = "3.1.3";
    static final String INITIAL_INSTALL_PLUGIN_VERSION = "2.4";
    private static final String INSTALL_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    private static final String INSTALL_PLUGIN_ARTIFACT_ID = "maven-install-plugin";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    /**
     * Project store where dependencies are stored
     */
    @Parameter(defaultValue = ".store", required = true)
    private File projectStore;

    @Component
    private Maven maven;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    MavenSession session;

    @Component
    MavenExecutionRequestPopulator execRequestPopulator;

    @Component
    private ArtifactResolver artifactResolver;

    @Component
    PluginVersionResolver pluginVersionResolver;

    @Component
    private ProjectBuilder projectBuilder;

    @Parameter(defaultValue = "${project.build.directory}", required = true, readonly = true)
    private File buildDirectory;

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

    private ModelReader modelReader;
    private ModelWriter modelWriter;
    private ProjectArtifactFactory artifactFactory;
   
    @Inject
    public InstallProjectStoreMojo(ProjectArtifactFactory artifactFactory, ModelReader modelReader, ModelWriter modelWriter) {
        this.artifactFactory = artifactFactory;
        this.modelReader = modelReader;
        this.modelWriter = modelWriter;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!projectStore.exists()) {
            return;
        }
        String installPluginVersion = computeMavenInstallPluginVersion();
        Set<Artifact> projectArtifacts = getProjectArtifacts();
        for (Artifact artifact : projectArtifacts) {
            File artifactFile = findFileInProjectStore(artifact);
            if (artifactFile.exists()) {
                installFileToLocalRepository(artifact, artifactFile, installPluginVersion);
            }
        }
    }

    private void installFileToLocalRepository(Artifact artifact, File artifactFile, String installPluginVersion) throws MojoExecutionException {
        if (artifact.isSnapshot()) { // Always update SNAPSHOT dependencies
            try {
                installArtifact(artifact, artifactFile, installPluginVersion);
            } catch (InstallFileExecutionException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
            try {
                artifactResolver.resolveArtifact(buildingRequest, artifact);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException(String.format("Failed to resolve artifact %s", artifact), e);
            } catch (ArtifactResolverException e) {
                try {
                    installArtifact(artifact, artifactFile, installPluginVersion);
                } catch (InstallFileExecutionException ex) {
                    throw new MojoExecutionException(ex.getMessage(), ex);
                }
            }
        }
    }

    String computeMavenInstallPluginVersion() throws MojoExecutionException {
        String installPluginVersion = DEFAULT_INSTALL_PLUGIN_VERSION;
        try {
            installPluginVersion = resolveInstallFilePluginVersion();
            if (INITIAL_INSTALL_PLUGIN_VERSION.equals(installPluginVersion)) {
                installPluginVersion = DEFAULT_INSTALL_PLUGIN_VERSION;
            }
        } catch (PluginVersionResolutionException e) {
            throw new MojoExecutionException("Failed to resolve the install plugin version.", e);
        }
        return installPluginVersion;
    }

    private Set<Artifact> getProjectArtifacts() throws MojoExecutionException {
        try {
           return artifactFactory.createArtifacts(project);
        } catch (InvalidDependencyVersionException e) {
           throw new MojoExecutionException(e);
        }
    }

    private void installArtifact(Artifact artifact, File artifactFile, String installPluginVersion)
            throws InstallFileExecutionException {
        File pomFile = null;
        try {
            if (shouldCreateDummyPomFile(artifactFile)) {
                pomFile = createDummyPomFile(artifact);
            }
            MavenExecutionResult executionResult = maven
                    .execute(newInstallFileExecutionRequest(artifact, artifactFile, pomFile, installPluginVersion));
            if (executionResult.hasExceptions()) {
                throw new InstallFileExecutionException(executionResult.getExceptions());
            }
        } catch (IOException | MavenExecutionRequestPopulationException e) {
            throw new InstallFileExecutionException("Failed to create artifact a pom file.", e);
        } finally {
            try {
                if (pomFile != null) {
                    Files.deleteIfExists(pomFile.toPath());
                }
            } catch (IOException e) {
                getLog().error(e);
            }
        }

    }

    private boolean shouldCreateDummyPomFile(File artifactFile) throws IOException {
        Optional<Model> existingPom = findPomFile(artifactFile);
        if (existingPom.isPresent()) {
            var pomFile = Files.createTempFile("pom", ".xml").toFile();
            try (var fos = new FileOutputStream(pomFile)) {
                Model model = existingPom.orElseThrow();
                modelWriter.write(fos, null, model);
                ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
                buildingRequest.setProcessPlugins(false);
                buildingRequest.setResolveDependencies(true);
                buildingRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                ProjectBuildingResult buildingResult = projectBuilder
                        .build(pomFile, buildingRequest);
                DependencyResolutionResult dependencyResolutionResult = buildingResult.getDependencyResolutionResult();
                return !dependencyResolutionResult.getUnresolvedDependencies().isEmpty();
            } catch (ProjectBuildingException e) {
                return true;
            } finally {
                Files.deleteIfExists(pomFile.toPath());
            }
        }
        return false;
    }

    static String getExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index != -1 && fileName.length() > index + 1) {
            return fileName.substring(index + 1, fileName.length());
        }
        return null;
    }

    private Optional<Model> findPomFile(File artifactFile) {
        String extension = getExtension(artifactFile.getName());
        if ("jar".equalsIgnoreCase(extension)) {
            String fileName = artifactFile.getName();
            String fileNameWithoutExtension = fileName.substring(0, fileName.length() - 4);
            try (JarFile jarFile = new JarFile(artifactFile)) {
                return jarFile.stream()
                        .filter(entry -> entry.getName()
                                .matches("META-INF/maven/[^/]*/[^/]*/pom.properties"))
                        .map(entry -> {
                            var properties = new Properties();
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                properties.load(is);
                                return properties;
                            } catch (IOException e) {
                                getLog().error(e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .filter(properties -> {
                            String artifactId = properties.getProperty(ARTIFACT_ID);
                            String version = properties.getProperty(VERSION);
                            return fileNameWithoutExtension.equals(String.format("%s-%s", artifactId, version))
                                    || fileNameWithoutExtension.equals(artifactId);
                        })
                        .findFirst()
                        .map(pomProperties -> {
                            ZipEntry pomEntry = jarFile.getEntry(String.format("META-INF/maven/%s/%s/pom.xml",
                                    pomProperties.getProperty(GROUP_ID),
                                    pomProperties.getProperty(ARTIFACT_ID)));
                            try (InputStream is = jarFile.getInputStream(pomEntry)) {
                                return modelReader.read(is, null);
                            } catch (IOException e) {
                                getLog().error(e);
                                return null;
                            }
                        });
            } catch (IOException e) {
                getLog().error("Failed to read jar " + artifactFile.getName(), e);
            }
        }
        return Optional.empty();
    }

    private File createDummyPomFile(Artifact artifact) throws IOException {
        File workdir = buildDirectory.toPath().resolve("install-plugin-workdir").toFile();
        if (!workdir.exists()) {
            workdir.mkdirs();
        }
        Path pomFile = Files.createTempFile(workdir.toPath(), "pom", ".xml");
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(artifact.getGroupId());
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging(artifact.getType());
        try (OutputStream os = Files.newOutputStream(pomFile)) {
            modelWriter.write(os, null, model);
        }
        return pomFile.toFile();
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

    MavenExecutionRequest newInstallFileExecutionRequest(Artifact artifact,
            File artifactFile,
            File pomFile,
            String installPluginVersion) throws MavenExecutionRequestPopulationException {
        MavenExecutionRequest executionRequest = new DefaultMavenExecutionRequest();
        // Retrieve mirrors, proxies and repositories settings. Removed in Maven 4.
        execRequestPopulator.populateFromSettings(executionRequest, session.getSettings());
        executionRequest.setGoals(Arrays.asList(String.format("%s:%s:%s:install-file",
                INSTALL_PLUGIN_GROUP_ID,
                INSTALL_PLUGIN_ARTIFACT_ID,
                installPluginVersion)));
        executionRequest.setLocalRepository(localRepository);
        executionRequest.setSystemProperties(System.getProperties());
        Properties installFileProperties = new Properties();
        installFileProperties.setProperty(GROUP_ID, artifact.getGroupId());
        installFileProperties.setProperty(ARTIFACT_ID, artifact.getArtifactId());
        installFileProperties.setProperty(VERSION, artifact.getVersion());
        installFileProperties.setProperty("file", artifactFile.getAbsolutePath());
        if (pomFile != null) {
            installFileProperties.setProperty("pomFile", pomFile.getAbsolutePath());
        }
        if (artifact.getClassifier() != null) {
            installFileProperties.setProperty("classifier", artifact.getClassifier());
        }
        installFileProperties.setProperty("packaging", artifact.getType());
        executionRequest.setUserProperties(installFileProperties);
        return executionRequest;
    }

    String resolveInstallFilePluginVersion() throws PluginVersionResolutionException {
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
        buildingRequest.setSystemProperties(System.getProperties());

        return buildingRequest;
    }

}
