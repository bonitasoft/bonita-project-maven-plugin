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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.bonitasoft.bonita2bar.BuildDiagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuildBarMojoTest {

    @Mock
    private MavenProjectHelper projectHelper;
    @Mock
    private Log log;

    private BuildBarMojo mojo;

    @BeforeEach
    void createMojo() throws Exception {
        mojo = new BuildBarMojo(projectHelper);
        mojo.setLog(log);
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

    @Test
    void reportDiagnosticsLogsEachWarning() throws Exception {
        mojo.reportDiagnostics(List.of(
                BuildDiagnostic.warning("xmlbeans-5.0.3.jar resolved as 5.4.0"),
                BuildDiagnostic.warning("guava-31.1-jre.jar resolved as 33.0.0-jre")));

        verify(log).warn("xmlbeans-5.0.3.jar resolved as 5.4.0");
        verify(log).warn("guava-31.1-jre.jar resolved as 33.0.0-jre");
        verifyNoMoreInteractions(log);
    }

    @Test
    void reportDiagnosticsOnlyWarnsByDefault() {
        assertThatCode(() -> mojo.reportDiagnostics(List.of(BuildDiagnostic.warning("a mismatch"))))
                .doesNotThrowAnyException();
    }

    @Test
    void reportDiagnosticsFailsOnWarningWhenConfiguredTo() {
        mojo.failOnDependencyMismatch = true;

        assertThatThrownBy(() -> mojo.reportDiagnostics(List.of(
                BuildDiagnostic.warning("a mismatch"),
                BuildDiagnostic.warning("another one"))))
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("2 dependency issue(s)")
                .hasMessageContaining("bonita.failOnDependencyMismatch");
    }

    @Test
    void reportDiagnosticsDoesNotFailWithoutWarning() {
        mojo.failOnDependencyMismatch = true;

        assertThatCode(() -> mojo.reportDiagnostics(List.of())).doesNotThrowAnyException();
        verifyNoMoreInteractions(log);
    }

}
