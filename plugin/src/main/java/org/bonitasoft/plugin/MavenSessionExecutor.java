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
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
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
public class MavenSessionExecutor {

    /**
     * Exception thrown when the maven build fails.
     */
    public static final class BuildException extends Exception {

        private static final long serialVersionUID = 1L;

        public BuildException(String message, Throwable cause) {
            super(message, cause);
        }

    }

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

    /**
     * Execute maven commands on a pom file.
     * 
     * @param pomFile the pom file
     * @param rootModuleDirectory the root module directory, used to set the base directory for multi-module builds
     * @param goals the goals to execute
     * @param properties user properties to pass as -D arguments
     * @param activeProfiles the active profiles to use
     * @param errorMessageBase a supplier of the base error message to use in case of failure
     * @throws BuildException if an error occurs or the execution fails
     */
    public void execute(File pomFile, File rootModuleDirectory, List<String> goals, Map<String, String> properties,
            List<String> activeProfiles, Supplier<String> errorMessageBase) throws BuildException {
        execute(pomFile, rootModuleDirectory, goals, properties, List.of(), activeProfiles, errorMessageBase);
    }

    /**
     * Execute maven commands on a pom file.
     * 
     * @param pomFile the pom file
     * @param rootModuleDirectory the root module directory, used to set the base directory for multi-module builds
     * @param goals the goals to execute
     * @param properties user properties to pass as -D arguments
     * @param extraArguments additional arguments to pass to the maven command (such as "-fn")
     * @param activeProfiles the active profiles to use
     * @param errorMessageBase a supplier of the base error message to use in case of failure
     * @throws BuildException if an error occurs or the execution fails
     */
    public void execute(File pomFile, File rootModuleDirectory, List<String> goals, Map<String, String> properties,
            List<String> extraArguments,
            List<String> activeProfiles, Supplier<String> errorMessageBase) throws BuildException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomFile);
        request.addArgs(goals);
        Stream<String> propArguments = properties.entrySet().stream().map(e -> "-D" + e.getKey() + "=" + e.getValue());
        request.addArgs(propArguments.toList());
        request.addArgs(extraArguments);
        request.setBaseDirectory(pomFile.getParentFile());
        request.setQuiet(true);
        request.setBatchMode(true);

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
            invokeMaven(rootModuleDirectory, errorMessageBase, request, errStream, errPrintStream);
        } catch (IOException e) {
            throw new BuildException(errorMessageBase.get(), e);
        }

    }

    private void invokeMaven(File rootModuleDirectory, Supplier<String> errorMessageBase, InvocationRequest request,
            ByteArrayOutputStream errStream, PrintStream errPrintStream) throws BuildException, IOException {
        try {
            String mvnHome = Optional.ofNullable(System.getProperty("maven.home")).orElse("");
            if (mvnHome.endsWith("\\EMBEDDED") || mvnHome.endsWith("/EMBEDDED")) {
                // we are in embedded mode or miss the executable and can not use invoker because the maven home is not a folder
                String msg = "Embedded maven home.";
                throw new MavenInvocationException(msg, new CommandLineConfigurationException(msg));
            } else {
                Invoker invoker = new DefaultInvoker();
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    throwBuildException(errorMessageBase, errStream, result.getExecutionException());
                }
            }
        } catch (MavenInvocationException e) {
            if (e.getCause() instanceof CommandLineConfigurationException) {
                invokeMavenCli(rootModuleDirectory, errorMessageBase, request, errStream, errPrintStream);
            } else {
                throwBuildException(errorMessageBase, errStream, e);
            }
        }
    }

    private void invokeMavenCli(File rootModuleDirectory, Supplier<String> errorMessageBase, InvocationRequest request,
            ByteArrayOutputStream errStream, PrintStream errPrintStream) throws BuildException, IOException {
        // try and use the maven cli class
        @SuppressWarnings("deprecation")
        MavenCli cli = new MavenCli(session.getContainer().getContainerRealm().getWorld());
        String oldMultimoduleProjectProperty = System.getProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        try {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, rootModuleDirectory.toURI().toString());
            var exitCode = cli.doMain(request.getArgs().toArray(String[]::new),
                    request.getBaseDirectory().getAbsolutePath(), null, errPrintStream);
            if (exitCode != 0) {
                throwBuildException(errorMessageBase, errStream, null);
            }
        } finally {
            // restore the basedir property
            if (oldMultimoduleProjectProperty == null) {
                System.clearProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
            } else {
                System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, oldMultimoduleProjectProperty);
            }
        }
    }

    /**
     * Build and throw the exception
     * 
     * @param errorMessageBase the base message supplier
     * @param errorStream the error stream from maven output
     * @param exception cause exception to encapsulate
     * @throws BuildException thrown exception
     * @throws IOException exception while flushing the error stream
     */
    private void throwBuildException(Supplier<String> errorMessageBase, ByteArrayOutputStream errorStream,
            Exception exception) throws BuildException, IOException {
        StringBuffer msg = new StringBuffer(errorMessageBase.get());
        errorStream.flush();
        String fromStream = errorStream.toString();
        Optional.ofNullable(fromStream).filter(StringUtils::isNotBlank).ifPresent(s -> msg.append("\n" + s));
        throw new BuildException(msg.toString(), exception);
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

    /**
     * Get the maven executor from the maven session
     * 
     * @param session maven session
     * @return executor relying on the session
     */
    public static MavenExecutor forBarFromSession(MavenSession session) {
        MavenSessionExecutor executor = fromSession(session);
        return (pomFile, goals, properties, activeProfiles, errorMessageBase) -> {
            try {
                // for bar, this is always executed on the app module, child of the root module
                var rootModule = pomFile.getParentFile().getParentFile();
                executor.execute(pomFile, rootModule, goals, properties, activeProfiles, errorMessageBase);
            } catch (BuildException e) {
                throw new BuildBarException(errorMessageBase.get(), e);
            }
        };
    }

}
