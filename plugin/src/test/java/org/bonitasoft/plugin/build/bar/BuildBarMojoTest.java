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
package org.bonitasoft.plugin.build.bar;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuildBarMojoTest {

    @Mock
    private MavenProjectHelper projectHelper;

    private BuildBarMojo mojo;

    @BeforeEach
    void createMojo() throws Exception {
        mojo = new BuildBarMojo(projectHelper);
    }

    @Test
    void defaultConfigurationFileName() throws Exception {
        var project = new MavenProject();
        project.setArtifactId("hello");
        project.setVersion("1.0.0");
        mojo.environment = "local";

        assertThat(mojo.getConfigurationFileName(project)).isEqualTo("hello-1.0.0-local.bconf");
    }

    @Test
    void customConfigurationFileName() throws Exception {
        var project = new MavenProject();
        project.setArtifactId("hello");
        project.setVersion("1.0.0");
        mojo.environment = "Local";
        mojo.configurationFileName = "hello-1.0.0.bconf";

        assertThat(mojo.getConfigurationFileName(project)).isEqualTo("hello-1.0.0.bconf");
    }

}
