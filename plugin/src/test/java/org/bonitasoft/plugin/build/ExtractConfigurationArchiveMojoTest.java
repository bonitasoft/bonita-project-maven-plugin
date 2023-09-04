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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.bonita2bar.BarBuilder;
import org.bonitasoft.bonita2bar.BuildResult;
import org.bonitasoft.bonita2bar.configuration.ParameterConfigurationExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractConfigurationArchiveMojoTest {

    @Mock
    ParameterConfigurationExtractor extractor;

    @Mock
    BarBuilder barBuilder;

    @Mock(strictness = Strictness.LENIENT)
    BuildResult result;

    @Spy
    private ExtractConfigurationArchiveMojo mojo;

    @BeforeEach
    void setup(@TempDir Path tmpFolder) throws IOException, MojoExecutionException {
        mojo.environment = "Local";
        var project = aProject("org.bonitasoft.example", "my-project", "1.0.0-SNAPSHOT");
        mojo.project = project;
        mojo.parametersFile = tmpFolder.resolve("parameters-local.yml").toString();
        mojo.reactorProjects = List.of(project);
        doReturn(tmpFolder.resolve("bconf")).when(mojo).getTempFolder();
        doReturn(barBuilder).when(mojo).createBarBuilder(any());
        when(barBuilder.buildConfiguration(any())).thenReturn(result);
        doAnswer(invocation -> {
            Path confFile = invocation.getArgument(0);
            // Add test bconf in application output
            var bconfFile = new File(
                    ExtractConfigurationArchiveMojoTest.class.getResource("/my-project-1.0.0-SNAPSHOT-local.bconf")
                            .getFile());
            Files.createDirectories(confFile.getParent());
            Files.copy(bconfFile.toPath(), confFile);
            return null;
        }).when(result).writeBonitaConfigurationTo(any());
    }

    @Test
    void execute() throws Exception {
        mojo.execute();

        assertThat(new File(mojo.parametersFile)).exists();
    }

    @Test
    void executeFailsWhenExtractorFails() throws Exception {
        mojo.extractor = extractor;
        willThrow(IOException.class).given(extractor).extract(any(), any(), any(boolean.class));

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void executeFailsWhenParametersFileAlreadyExists() throws Exception {
        new File(mojo.parametersFile).createNewFile();

        assertThrows(MojoFailureException.class, () -> mojo.execute());
    }

    @Test
    void executeFailsBuildConfigurationFails() throws Exception {
        when(barBuilder.buildConfiguration(any())).thenThrow(new IOException("Error"));

        assertThrows(MojoFailureException.class, () -> mojo.execute());
    }

    @Test
    void executeOverrideExistingParameterFile() throws Exception {
        mojo.overwrite = true;

        mojo.execute();

        assertThat(new File(mojo.parametersFile)).exists();
    }

    @Test
    void executeWithoutConfiguration() throws Exception {
        doNothing().when(result).writeBonitaConfigurationTo(any());

        mojo.execute();

        assertThat(new File(mojo.parametersFile)).doesNotExist();
    }

    private static MavenProject aProject(String groupId, String artifactId, String version) throws IOException {
        var aProject = new MavenProject();
        aProject.setGroupId(groupId);
        aProject.setArtifactId(artifactId);
        aProject.setVersion(version);
        return aProject;
    }

}
