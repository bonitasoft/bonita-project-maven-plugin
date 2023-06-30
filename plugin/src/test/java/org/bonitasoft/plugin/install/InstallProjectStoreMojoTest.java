/** 
 * Copyright (C) 2021 BonitaSoft S.A.
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
package org.bonitasoft.plugin.install;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.DefaultMavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.ProjectArtifactFactory;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.repository.ArtifactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstallProjectStoreMojoTest {

    MavenExecutionRequestPopulator populator = new DefaultMavenExecutionRequestPopulator(new MavenRepositorySystem());

    @Mock
    MavenSession session;

    @Mock
    PluginVersionResolver pluginVersionResolver;

    @Mock
    ProjectArtifactFactory projectArtifactFactory;

    @Test
    void should_return_file_extension() throws Exception {
        String extension = InstallProjectStoreMojo.getExtension("lib.jar");

        assertThat(extension).isEqualTo("jar");
    }

    @Test
    void should_return_null_file_extension_when_name_ending_with_a_dot() throws Exception {
        String extension = InstallProjectStoreMojo.getExtension("lib.");

        assertThat(extension).isNull();
    }

    @Test
    void should_return_null_file_extension() throws Exception {
        String extension = InstallProjectStoreMojo.getExtension("lib");

        assertThat(extension).isNull();
    }

    @Test
    void should_create_an_install_file_execution_request() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.execRequestPopulator = populator;
        mojo.session = session;
        var artifact = new DefaultArtifact("g", "a", "v", null, "jar", null, new DefaultArtifactHandler("jar"));

        File artifactFile = new File("artifact.jar");
        var request = mojo.newInstallFileExecutionRequest(artifact, artifactFile, null, "3.0.0");

        assertThat(request.getGoals()).containsOnly("org.apache.maven.plugins:maven-install-plugin:3.0.0:install-file");
        assertThat(request.getUserProperties()).containsEntry("groupId", "g")
                .containsEntry("artifactId", "a")
                .containsEntry("version", "v")
                .containsEntry("file", artifactFile.getAbsolutePath())
                .containsEntry("packaging", "jar");
    }

    @Test
    void should_create_an_install_file_execution_request_with_classifier() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.execRequestPopulator = populator;
        mojo.session = session;
        var artifact = new DefaultArtifact("g", "a", "v", null, "jar", "sources", new DefaultArtifactHandler("jar"));

        File artifactFile = new File("artifact.jar");
        var request = mojo.newInstallFileExecutionRequest(artifact, artifactFile, null, "3.0.0");

        assertThat(request.getUserProperties()).containsEntry("groupId", "g")
                .containsEntry("artifactId", "a")
                .containsEntry("version", "v")
                .containsEntry("classifier", "sources")
                .containsEntry("file", artifactFile.getAbsolutePath())
                .containsEntry("packaging", "jar");
    }

    @Test
    void should_create_an_install_file_execution_request_with_pom() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.execRequestPopulator = populator;
        mojo.session = session;
        var artifact = new DefaultArtifact("g", "a", "v", null, "jar", null, new DefaultArtifactHandler("jar"));

        File artifactFile = new File("artifact.jar");
        File pomFile = new File("pom.xml");
        var request = mojo.newInstallFileExecutionRequest(artifact, artifactFile, pomFile, "3.0.0");

        assertThat(request.getUserProperties()).containsEntry("groupId", "g")
                .containsEntry("artifactId", "a")
                .containsEntry("version", "v")
                .containsEntry("pomFile", pomFile.getAbsolutePath())
                .containsEntry("file", artifactFile.getAbsolutePath())
                .containsEntry("packaging", "jar");
    }

    @Test
    void should_create_an_install_file_execution_request_populated_with_sessions_setttings() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.execRequestPopulator = populator;
        var settings = new Settings();
        var mirror = new Mirror();
        mirror.setId("test-mirror");
        mirror.setMirrorOf("*");
        mirror.setUrl("http://test-mirror.net");
        settings.addMirror(mirror);
        when(session.getSettings()).thenReturn(settings);
        mojo.session = session;
        var artifact = new DefaultArtifact("g", "a", "v", null, "jar", null, new DefaultArtifactHandler("jar"));

        File artifactFile = new File("artifact.jar");
        File pomFile = new File("pom.xml");
        var request = mojo.newInstallFileExecutionRequest(artifact, artifactFile, pomFile, "3.0.0");

        assertThat(request.getMirrors()).extracting("id").contains("test-mirror");
    }

    @Test
    void should_use_the_default_install_plugin_version() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.pluginVersionResolver = pluginVersionResolver;
        mojo.project = new MavenProject();
        var result = new PluginVersionResult() {

            @Override
            public String getVersion() {
                return InstallProjectStoreMojo.INITIAL_INSTALL_PLUGIN_VERSION;
            }

            @Override
            public ArtifactRepository getRepository() {
                return null;
            }
        };
        when(pluginVersionResolver.resolve(any(PluginVersionRequest.class))).thenReturn(result);
        mojo.session = session;
        var version = mojo.computeMavenInstallPluginVersion();

        assertThat(version).isEqualTo(InstallProjectStoreMojo.DEFAULT_INSTALL_PLUGIN_VERSION);
    }

    @Test
    void should_use_the_project_defined_install_plugin_version() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.pluginVersionResolver = pluginVersionResolver;
        mojo.project = new MavenProject();
        var result = new PluginVersionResult() {

            @Override
            public String getVersion() {
                return "3.1.1";
            }

            @Override
            public ArtifactRepository getRepository() {
                return null;
            }
        };
        when(pluginVersionResolver.resolve(any(PluginVersionRequest.class))).thenReturn(result);
        mojo.session = session;
        var version = mojo.computeMavenInstallPluginVersion();

        assertThat(version).isEqualTo("3.1.1");
    }

    @Test
    void should_throw_MojoExecutionException_when_plugin_version_resolution_fails() throws Exception {
        var mojo = new InstallProjectStoreMojo(projectArtifactFactory, new DefaultModelReader(),
                new DefaultModelWriter());
        mojo.pluginVersionResolver = pluginVersionResolver;
        mojo.project = new MavenProject();
        when(pluginVersionResolver.resolve(any(PluginVersionRequest.class))).thenThrow(
                new PluginVersionResolutionException("org.apache.maven.plugins", "maven-install-plugin", "2.4"));
        mojo.session = session;

        assertThrows(MojoExecutionException.class, () -> mojo.computeMavenInstallPluginVersion());
    }

}
