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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.bonita2bar.configuration.ParameterConfigurationExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractConfigurationArchiveMojoTest {

    @Mock
    ParameterConfigurationExtractor extractor;

    private ExtractConfigurationArchiveMojo mojo;

    @BeforeEach
    void setup(@TempDir Path tmpFolder) throws IOException {
        mojo = new ExtractConfigurationArchiveMojo();
        mojo.environment = "Local";
        mojo.outputFileName = "parameters-${bonita.environment}.yml";
        var project = aProject("org.bonitasoft.example", "my-project", "1.0.0-SNAPSHOT", tmpFolder,
                "/my-project-1.0.0-SNAPSHOT-Local.bconf");
        mojo.project = project;
        mojo.reactorProjects = List.of(project);
    }

    @Test
    void execute() throws Exception {
        mojo.execute();

        assertThat(new File(mojo.project.getBuild().getDirectory(), "parameters-Local.yml")).exists();
    }

    @Test
    void executeFailsWhenNoConfigurationArchive() throws Exception {
        mojo.bonitaConfiguration = "";

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void executeFailsWhenExtractorFails() throws Exception {
        mojo.extractor = extractor;
        willThrow(IOException.class).given(extractor).extract(any(), any(), any(boolean.class));

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    private static MavenProject aProject(String groupId, String artifactId, String version, Path buildFolder,
            String bconfTestResource) throws IOException {
        var aProject = new MavenProject();
        aProject.setGroupId(groupId);
        aProject.setArtifactId(artifactId);
        aProject.setVersion(version);
        var build = new Build();
        build.setDirectory(buildFolder.toString());
        build.setFinalName(String.format("%s-%s", artifactId, version));
        aProject.setBuild(build);
        // Add test bconf in build.dir
        var bconfFile = new File(
                ExtractConfigurationArchiveMojoTest.class.getResource(bconfTestResource).getFile());
        Files.copy(bconfFile.toPath(), buildFolder.resolve(bconfFile.getName()));
        return aProject;
    }

}
