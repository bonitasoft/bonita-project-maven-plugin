/**
 * Copyright (C) 2025 BonitaSoft S.A.
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
package org.bonitasoft.plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.bonitasoft.bonita2bar.BuildBarException;
import org.bonitasoft.bonita2bar.MavenExecutor;

/**
 * Executes maven requests with information from an active session.
 */
public class MavenSessionExecutor implements MavenExecutor {

    /** The maven session */
    private MavenSession session;

    /**
     * Private Constructor.
     * 
     * @param session the maven session
     */
    private MavenSessionExecutor(MavenSession session) {
        this.session = session;
    }

    @Override
    public void execute(File pomFile, List<String> goals, Map<String, String> properties, List<String> activeProfiles,
            Supplier<String> errorMessageBase) throws BuildBarException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.addArgs(goals);
        Stream<String> propArguments = properties.entrySet().stream().map(e -> "-D" + e.getKey() + "=" + e.getValue());
        request.addArgs(propArguments.toList());
        request.setBaseDirectory(pomFile.getParentFile());
        request.setQuiet(true);

        // information from the session
        // no need to report system properties that will make the command line too long and fail on Windows
        session.getUserProperties().forEach((key, value) -> {
            // avoid conflict with explicit properties
            if (!properties.containsKey(key)) {
                request.addArg("-D" + key + "=" + value);
            }
        });
        request.setGlobalSettingsFile(session.getRequest().getGlobalSettingsFile());
        request.setUserSettingsFile(session.getRequest().getUserSettingsFile());
        request.setLocalRepositoryDirectory(session.getRequest().getLocalRepositoryPath());

        // mixed information
        Set<String> profiles = new HashSet<>(activeProfiles);
        profiles.addAll(session.getRequest().getActiveProfiles());
        request.setProfiles(new ArrayList<>(profiles));

        try (var errStream = new ByteArrayOutputStream();
                var errPrintStream = new PrintStream(errStream);) {
            var errHandler = new PrintStreamHandler(errPrintStream, false);
            request.setErrorHandler(errHandler);
            try {
                Invoker invoker = new DefaultInvoker();
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    throwBuildBarException(errorMessageBase, errStream, result.getExecutionException());
                }
            } catch (MavenInvocationException e) {
                throwBuildBarException(errorMessageBase, errStream, e);
            }
        } catch (IOException e) {
            throw new BuildBarException(errorMessageBase.get(), e);
        }

    }

    /**
     * Build and throw the exception
     * 
     * @param errorMessageBase the base message supplier
     * @param errorStream the error stream from maven output
     * @param exception cause exception to encapsulate
     * @throws BuildBarException thrown exception
     * @throws IOException exception while flushing the error stream
     */
    private void throwBuildBarException(Supplier<String> errorMessageBase, ByteArrayOutputStream errorStream,
            Exception exception) throws BuildBarException, IOException {
        StringBuffer msg = new StringBuffer(errorMessageBase.get());
        errorStream.flush();
        String fromStream = errorStream.toString();
        Optional.ofNullable(fromStream).filter(StringUtils::isNotBlank).ifPresent(s -> msg.append("\n" + s));
        throw new BuildBarException(msg.toString(), exception);
    }

    /**
     * Get the maven executor from the maven session
     * 
     * @param session maven session
     * @return executor relying on the session
     */
    public static MavenSessionExecutor fromSession(MavenSession session) {
        return new MavenSessionExecutor(session);
    }

}
