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
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationPropertiesMojoTest {

    @Test
    void execute(@TempDir Path output) throws Exception {
        var mojo = new ApplicationPropertiesMojo();
        mojo.project = new MavenProject();
        mojo.project.setVersion("1.0.0");
        var build = new Build();
        build.setDirectory(output.toString());
        mojo.project.setBuild(build);

        mojo.execute();

        assertThat(output.resolve("application.properties")).exists();
        assertThat(load(output.resolve("application.properties"))).containsEntry("version", "1.0.0");
    }

    @Test
    void executeWithIOExceptionCreatingOuputDir(@TempDir Path output) throws Exception {
        var mojo = spy(new ApplicationPropertiesMojo());
        mojo.project = new MavenProject();
        mojo.project.setVersion("1.0.0");
        var build = new Build();
        build.setDirectory(output.toString());
        mojo.project.setBuild(build);
        doThrow(IOException.class).when(mojo).getOuputDir();

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    @Test
    void executeWithIOExceptionWritingProperties(@TempDir Path output) throws Exception {
        var mojo = spy(new ApplicationPropertiesMojo());
        mojo.project = new MavenProject();
        mojo.project.setVersion("1.0.0");
        var build = new Build();
        build.setDirectory(output.toString());
        mojo.project.setBuild(build);
        doThrow(IOException.class).when(mojo).writeApplicationProperties(any(), any());

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

    private Properties load(Path path) throws IOException {
        var properties = new Properties();
        try (var is = Files.newInputStream(path)) {
            properties.load(is);
        }
        return properties;
    }
}
