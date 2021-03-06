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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
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

import org.apache.maven.Maven;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
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
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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

    @Component
    private ProjectBuilder projectBuilder;

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

    private MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
    private MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();

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
                } catch (InstallFileExecutionException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
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
                    throw new MojoExecutionException(ex.getMessage(), ex);
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
        } catch (IOException e) {
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
            try (StringWriter stringWriter = new StringWriter()) {
                Model model = existingPom.orElseThrow();
                mavenXpp3Writer.write(stringWriter, model);
                ProjectBuildingRequest buildingRequest = newResolveArtifactProjectBuildingRequest();
                buildingRequest.setProcessPlugins(false);
                buildingRequest.setResolveDependencies(true);
                buildingRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
                ProjectBuildingResult buildingResult = projectBuilder
                        .build(new StringModelSource(stringWriter.toString()), buildingRequest);
                DependencyResolutionResult dependencyResolutionResult = buildingResult.getDependencyResolutionResult();
                return !dependencyResolutionResult.getUnresolvedDependencies().isEmpty();
            }catch (ProjectBuildingException e) {
                return true;
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
                                .matches("META-INF/maven/[^/]*/[^/]*/pom.xml"))
                        .map(entry -> {
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                return mavenXpp3Reader.read(is);
                            } catch (IOException | XmlPullParserException e) {
                                getLog().error(e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .filter(model -> {
                            String artifactId = model.getArtifactId();
                            String version = model.getVersion();
                            if (version == null && model.getParent() != null) {
                                version = model.getParent().getVersion();
                            }
                            return fileNameWithoutExtension.equals(String.format("%s-%s", artifactId, version))
                                    || fileNameWithoutExtension.equals(artifactId);
                        })
                        .findFirst();
            } catch (IOException e) {
                getLog().error("Failed to read jar " + artifactFile.getName(), e);
            }
        }
        return Optional.empty();
    }

    private File createDummyPomFile(Artifact artifact) throws IOException {
        Path pomFile = Files.createTempFile("pom", ".xml");
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(artifact.getGroupId());
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging(artifact.getType());
        try (OutputStream os = Files.newOutputStream(pomFile)) {
            mavenXpp3Writer.write(os, model);
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

    private MavenExecutionRequest newInstallFileExecutionRequest(Artifact artifact,
            File artifactFile,
            File pomFile,
            String installPluginVersion) {
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
