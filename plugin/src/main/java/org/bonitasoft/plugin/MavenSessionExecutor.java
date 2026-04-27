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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.cli.MavenCli;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
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
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

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

    /** Logger for Maven-style logging */
    private Log log = new SystemStreamLog();

    /** Project builder for parsing POM files */
    private ProjectBuilder projectBuilder;

    /**
     * Private Constructor.
     *
     * @param session the maven session
     */
    private MavenSessionExecutor(MavenSession session) {
        this.session = session;
    }

    /**
     * Set the Maven logger.
     *
     * @param log the Maven log
     * @return this executor for chaining
     */
    public MavenSessionExecutor withLog(Log log) {
        this.log = log;
        return this;
    }

    /**
     * Set the project builder.
     *
     * @param projectBuilder the project builder
     * @return this executor for chaining
     */
    public MavenSessionExecutor withProjectBuilder(ProjectBuilder projectBuilder) {
        this.projectBuilder = projectBuilder;
        return this;
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

        try (var outStream = new ByteArrayOutputStream();
                var outPrintStream = new PrintStream(outStream);) {
            var outHandler = new PrintStreamHandler(outPrintStream, false);
            request.setErrorHandler(outHandler);
            request.setOutputHandler(outHandler);
            invokeMaven(rootModuleDirectory, errorMessageBase, request, outStream, outPrintStream);
        } catch (IOException e) {
            throw new BuildException(errorMessageBase.get(), e);
        }

    }

    private void invokeMaven(File rootModuleDirectory, Supplier<String> errorMessageBase, InvocationRequest request,
            ByteArrayOutputStream outStream, PrintStream outPrintStream) throws BuildException, IOException {
        try {
            String mvnHome = Optional.ofNullable(System.getProperty("maven.home")).orElse("");
            if (mvnHome.endsWith("\\EMBEDDED") || mvnHome.endsWith("/EMBEDDED")) {
                // we are in embedded mode or miss the executable and can not use invoker because the maven home is not a folder
                String msg = "Embedded maven home.";
                throw new MavenInvocationException(msg, new CommandLineConfigurationException(msg));
            } else {
                log.debug("Executing Maven request " + request.getArgs() + " on POM " + request.getPomFile());
                Invoker invoker = new DefaultInvoker();
                InvocationResult result = invoker.execute(request);
                if (result.getExitCode() != 0) {
                    log.debug("Maven execution failed with exit code " + result.getExitCode());
                    throwBuildException(errorMessageBase, outStream, result.getExecutionException());
                }
            }
        } catch (MavenInvocationException e) {
            if (e.getCause() instanceof CommandLineConfigurationException) {
                log.debug("Falling back to Maven CLI execution due to: " + e.getCause().getMessage());
                invokeMavenCli(rootModuleDirectory, errorMessageBase, request, outStream, outPrintStream);
            } else {
                throwBuildException(errorMessageBase, outStream, e);
            }
        }
    }

    private void invokeMavenCli(File rootModuleDirectory, Supplier<String> errorMessageBase, InvocationRequest request,
            ByteArrayOutputStream outStream, PrintStream outPrintStream) throws BuildException, IOException {
        // try and use the maven cli class
        @SuppressWarnings("deprecation")
        MavenCli cli = new MavenCli(session.getContainer().getContainerRealm().getWorld());
        String oldMultimoduleProjectProperty = System.getProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY);
        try {
            System.setProperty(MavenCli.MULTIMODULE_PROJECT_DIRECTORY, rootModuleDirectory.toURI().toString());
            var exitCode = cli.doMain(request.getArgs().toArray(String[]::new),
                    request.getBaseDirectory().getAbsolutePath(), null, outPrintStream);
            if (exitCode != 0) {
                throwBuildException(errorMessageBase, outStream, null);
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
     * @param outStream the error stream from maven output
     * @param exception cause exception to encapsulate
     * @throws BuildException thrown exception
     * @throws IOException exception while flushing the error stream
     */
    private void throwBuildException(Supplier<String> errorMessageBase, ByteArrayOutputStream outStream,
            Exception exception) throws BuildException, IOException {
        StringBuffer msg = new StringBuffer(errorMessageBase.get());
        outStream.flush();
        String fromStream = outStream.toString();
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
     * Get the maven executor for building business archives.
     *
     * @return executor for bar building
     */
    public MavenExecutor forBarBuild() {
        return (pomFile, goals, properties, activeProfiles, errorMessageBase) -> {
            try {
                // Install only reactor artifacts that are dependencies of this POM
                // This ensures that dependencies on reactor modules can be resolved
                // when building the temporary process project
                installReactorArtifactsForPom(pomFile);

                // Use the multi-module project directory for proper reactor support
                // This corresponds to maven.multiModuleProjectDirectory in fallback CLI mode
                File multiModuleProjectDir = session.getRequest().getMultiModuleProjectDirectory();
                execute(pomFile, multiModuleProjectDir, goals, properties, activeProfiles, errorMessageBase);
            } catch (BuildException e) {
                throw new BuildBarException(errorMessageBase.get(), e);
            }
        };
    }

    /**
     * Install reactor artifacts that are dependencies of the given POM file.
     * Only installs artifacts that are both in the reactor and listed as dependencies.
     *
     * @param pomFile the POM file to analyze for dependencies
     * @throws BuildException if installation fails
     */
    private void installReactorArtifactsForPom(File pomFile) throws BuildException {
        List<MavenProject> reactorProjects = session.getProjects();
        if (reactorProjects == null || reactorProjects.isEmpty()) {
            return;
        }

        // Build a map of reactor projects by groupId:artifactId for quick lookup
        Map<String, MavenProject> reactorProjectMap = reactorProjects.stream()
                .filter(p -> !"pom".equals(p.getPackaging()))
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getGroupId() + ":" + p.getArtifactId(),
                        p -> p,
                        (p1, p2) -> p1));

        if (reactorProjectMap.isEmpty()) {
            return;
        }

        // Parse the POM file to find dependencies that match reactor projects
        Set<String> requiredArtifacts = findReactorDependencies(pomFile, reactorProjectMap.keySet());
        if (requiredArtifacts.isEmpty()) {
            return;
        }

        log.info("Installing reactor artifacts to resolve dependencies...");
        for (String artifactKey : requiredArtifacts) {
            MavenProject project = reactorProjectMap.get(artifactKey);
            if (project == null) {
                continue;
            }

            File artifactFile = getProjectArtifactFile(project);
            if (artifactFile != null && artifactFile.exists()) {
                // Only install if not already in the local repository or if the file is newer
                File localRepoFile = getLocalRepositoryFile(project);
                if (!localRepoFile.exists() || artifactFile.lastModified() > localRepoFile.lastModified()) {
                    log.info("Installing " + project.getGroupId() + ":" +
                            project.getArtifactId() + ":" + project.getVersion());
                    installArtifact(project, artifactFile);
                }
            }
        }
    }

    /**
     * Find dependencies from the POM file that match reactor project keys.
     * Uses ProjectBuilder to parse the POM file.
     *
     * @param pomFile the POM file to parse
     * @param reactorKeys set of "groupId:artifactId" keys for reactor projects
     * @return set of matching reactor dependency keys
     * @throws BuildException if POM parsing fails
     */
    private Set<String> findReactorDependencies(File pomFile, Set<String> reactorKeys) throws BuildException {
        try {
            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
                    session.getProjectBuildingRequest());
            buildingRequest.setProcessPlugins(false);
            buildingRequest.setResolveDependencies(false);
            MavenProject project = projectBuilder.build(pomFile, buildingRequest).getProject();

            return project.getDependencies().stream()
                    .map(dep -> dep.getGroupId() + ":" + dep.getArtifactId())
                    .filter(reactorKeys::contains)
                    .collect(Collectors.toSet());
        } catch (ProjectBuildingException e) {
            throw new BuildException("Could not parse POM file for dependencies: " + pomFile, e);
        }
    }

    /**
     * Get the artifact file for a project.
     * Uses project.getArtifact().getFile() from Maven API.
     *
     * @param project the maven project
     * @return the artifact file, or null if not found
     */
    private File getProjectArtifactFile(MavenProject project) {
        if (project.getArtifact() != null && project.getArtifact().getFile() != null) {
            return project.getArtifact().getFile();
        }
        return null;
    }

    /**
     * Get the expected local repository file path for a project artifact.
     * Uses RepositorySystemSession and LocalRepositoryManager from Maven Resolver API.
     *
     * @param project the maven project
     * @return the file in the local repository
     */
    private File getLocalRepositoryFile(MavenProject project) {
        RepositorySystemSession repoSession = session.getRepositorySession();
        LocalRepositoryManager localRepoManager = repoSession.getLocalRepositoryManager();

        // Convert Maven Artifact to Aether Artifact
        org.apache.maven.artifact.Artifact mavenArtifact = project.getArtifact();
        org.eclipse.aether.artifact.Artifact aetherArtifact = new DefaultArtifact(
                mavenArtifact.getGroupId(),
                mavenArtifact.getArtifactId(),
                mavenArtifact.getClassifier(),
                mavenArtifact.getType(),
                mavenArtifact.getVersion());

        String path = localRepoManager.getPathForLocalArtifact(aetherArtifact);
        File localRepoBasedir = localRepoManager.getRepository().getBasedir();
        return new File(localRepoBasedir, path);
    }

    /**
     * Install an artifact to the local repository.
     * Uses PluginVersionResolver to resolve the maven-install-plugin version dynamically.
     *
     * @param project the maven project
     * @param artifactFile the artifact file to install
     * @throws BuildException if installation fails
     */
    private void installArtifact(MavenProject project, File artifactFile) throws BuildException {
        File pomFile = project.getFile();
        Map<String, String> properties = Map.of(
                "file", artifactFile.getAbsolutePath(),
                "groupId", project.getGroupId(),
                "artifactId", project.getArtifactId(),
                "version", project.getVersion(),
                "packaging", project.getPackaging(),
                "pomFile", pomFile.getAbsolutePath(),
                "generatePom", "false");

        try {
            execute(pomFile, pomFile.getParentFile(),
                    List.of("org.apache.maven.plugins:maven-install-plugin:install-file"), properties, List.of("-q"),
                    List.of(),
                    () -> "Failed to install reactor artifact " + project.getGroupId() + ":" + project.getArtifactId());
        } catch (BuildException e) {
            // Log but don't fail the build - the artifact might already be available
            // or the installation might not be critical
            log.warn("Could not install reactor artifact " +
                    project.getGroupId() + ":" + project.getArtifactId() + ": " + e.getMessage());
        }
    }

}
