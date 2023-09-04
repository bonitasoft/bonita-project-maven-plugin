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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.bonita2bar.configuration.ParametersConfigurationMerger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MergeConfigurationArchiveMojoTest {

    @Mock
    private ParametersConfigurationMerger merger;

    private MergeConfigurationArchiveMojo mojo;

    @BeforeEach
    void setup(@TempDir Path tmpFolder) throws IOException {
        mojo = new MergeConfigurationArchiveMojo();
        mojo.environment = "local";
        mojo.parametersFile = "parameters-${bonita.environment}.yml";
        var project = aProject("org.bonitasoft.example", "my-project", "1.0.0-SNAPSHOT", tmpFolder,
                "/my-project-1.0.0-SNAPSHOT-local.bconf", "local");
        mojo.project = project;
        mojo.reactorProjects = List.of(project);
    }

    @Test
    void execute() throws Exception {
        mojo.parametersFile = new File(
                MergeConfigurationArchiveMojoTest.class.getResource("/parameters-local.yml").getFile())
                .getAbsolutePath();

        mojo.execute();

        assertThat(new File(mojo.project.getBuild().getDirectory(), "my-project-1.0.0-SNAPSHOT-local.bconf")).exists();
    }

    @Test
    void executeGetEnvironmentFromProjectProperties() throws Exception {
        mojo.environment = null;
        mojo.project.getProperties().setProperty("bonita.environment", "local");
        mojo.parametersFile = new File(
                MergeConfigurationArchiveMojoTest.class.getResource("/parameters-local.yml").getFile())
                .getAbsolutePath();

        mojo.execute();

        assertThat(new File(mojo.project.getBuild().getDirectory(), "my-project-1.0.0-SNAPSHOT-local.bconf")).exists();
    }

    @Test
    void executeSkippedUsingProperty() throws Exception {
        mojo.merger = merger;
        mojo.skipMergeParameters = true;

        mojo.execute();

        verify(merger, never()).merge(any(), any(), any());
    }

    @Test
    void executeSkippedWhenNoInputParametersFileFound() throws Exception {
        mojo.merger = merger;

        mojo.execute();

        verify(merger, never()).merge(any(), any(), any());
    }

    @Test
    void executeWhenNoConfigurationArchive() throws Exception {
        mojo.parametersFile = new File(
                MergeConfigurationArchiveMojoTest.class.getResource("/parameters-local.yml").getFile())
                .getAbsolutePath();
        mojo.bonitaConfiguration = "";

        mojo.execute();

        verify(merger, never()).merge(any(), any(), any());
    }

    @Test
    void executeFailsWhenMergerFails() throws Exception {
        mojo.parametersFile = new File(
                MergeConfigurationArchiveMojoTest.class.getResource("/parameters-local.yml").getFile())
                .getAbsolutePath();
        mojo.merger = merger;
        doThrow(IOException.class).when(merger).merge(any(), any(), any());

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    private static MavenProject aProject(String groupId, String artifactId, String version, Path buildFolder,
            String bconfTestResource, String environement) throws IOException {
        var aProject = new MavenProject();
        aProject.setGroupId(groupId);
        aProject.setArtifactId(artifactId);
        aProject.setVersion(version);
        aProject.getProperties().setProperty("bonita.applicationOutput", buildFolder.toString());
        var build = new Build();
        build.setDirectory(buildFolder.toString());
        build.setFinalName(String.format("%s-%s", artifactId, version));
        aProject.setBuild(build);
        // Add test bconf in application output folder
        var bconfFile = new File(
                MergeConfigurationArchiveMojoTest.class.getResource(bconfTestResource).getFile());
        Path bConfFile = buildFolder.resolve(bconfFile.getName());
        Files.copy(bconfFile.toPath(), bConfFile);
        DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler("bconf");
        var artifact = new DefaultArtifact(groupId, artifactId, version, null, "bconf", environement, artifactHandler);
        artifact.setFile(bconfFile);
        aProject.getAttachedArtifacts().add(artifact);
        return aProject;
    }

}
