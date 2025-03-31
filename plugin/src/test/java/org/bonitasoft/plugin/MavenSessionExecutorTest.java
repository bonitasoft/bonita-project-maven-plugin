/** 
 * Copyright (C) 2025 BonitaSoft S.A.
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
package org.bonitasoft.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.bonitasoft.bonita2bar.BuildBarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenSessionExecutorTest {

    private File pomFile;
    private MavenExecutionRequest request;
    private MavenSession session;

    @BeforeEach
    void setUp(@TempDir File tempDir) throws IOException, InterruptedException {
        pomFile = new File(tempDir, "pom.xml");
        // write pom.xml content
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pomFile));) {
            writer.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>org.bonitasoft.fake</groupId>
                        <artifactId>test</artifactId>
                        <version>1.0.0</version>
                        <build>
                            <directory>target</directory>
                        </build>
                    </project>
                    """);
        }

        if (System.getProperty("maven.home") == null) {
            // get Maven home to set it in the system properties
            String os = System.getProperty("os.name");
            String mvn = os.startsWith("Windows") ? "mvn.cmd" : "mvn";
            ProcessBuilder builder = new ProcessBuilder(mvn, "help:evaluate", "-Dexpression=maven.home", "-q",
                    "-DforceStdout");
            // make sure correct Java version is used
            builder.environment().put("JAVA_HOME", System.getProperty("java.home"));
            Process process = builder.start();
            process.waitFor();
            String mavenHome = new String(process.getInputStream().readAllBytes());
            System.setProperty("maven.home", mavenHome);
        }

        request = mock(MavenExecutionRequest.class);
        Properties sysProps = new Properties();
        sysProps.putAll(System.getenv());
        sysProps.putAll(System.getProperties());
        when(request.getSystemProperties()).thenReturn(sysProps);
        session = mock(MavenSession.class);
        when(session.getUserProperties()).thenReturn(new Properties());
        when(session.getRequest()).thenReturn(request);
        when(session.getSystemProperties()).thenReturn(sysProps);
    }

    @Test
    void testSuccessfullMavenExecution() throws BuildBarException {
        // given
        MavenSessionExecutor executor = MavenSessionExecutor.fromSession(session);
        File target = new File(pomFile.getParent(), "target");
        target.mkdirs();
        assertThat(target).exists();

        List<String> goals = List.of("clean");
        Map<String, String> properties = Map.of();
        List<String> activeProfiles = List.of();
        Supplier<String> errorMessageBase = () -> "Error";

        // when
        executor.execute(pomFile, goals, properties, activeProfiles, errorMessageBase);

        // then
        assertThat(target).doesNotExist();
    }

    @Test
    void testFailedMavenExecution() {
        // given
        MavenSessionExecutor executor = MavenSessionExecutor.fromSession(session);

        List<String> goals = List.of("non-existing-goal");
        Map<String, String> properties = Map.of();
        List<String> activeProfiles = List.of();
        Supplier<String> errorMessageBase = () -> "Error";

        // when, then
        assertThatThrownBy(() -> executor.execute(pomFile, goals, properties, activeProfiles, errorMessageBase))
                .isInstanceOf(BuildBarException.class);
    }

}
