/** 
 * Copyright (C) 2026 BonitaSoft S.A.
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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.plugin.MojoExecutionException;
import org.bonitasoft.bonita2bar.BuildBarException;
import org.bonitasoft.bonita2bar.MavenExecutor;

/**
 * A {@link MavenExecutor} that resolves the specific {@code dependency:copy-dependencies} call made by
 * bonita2bar in-process (see {@link InProcessDependencyCopier}), avoiding the per-process Maven fork that
 * caused the BPA-834 performance regression. Any other goal/property combination falls back unchanged to
 * the given fork-based executor, so an unexpected future bonita2bar call shape degrades safely instead of
 * silently producing an incomplete result.
 */
public class BarMavenExecutor implements MavenExecutor {

    private static final List<String> COPY_DEPENDENCIES_GOAL = List.of("dependency:copy-dependencies");
    private static final String OUTPUT_DIRECTORY_PROPERTY = "outputDirectory";
    private static final String INCLUDE_SCOPE_PROPERTY = "includeScope";
    private static final String INCLUDE_TYPES_PROPERTY = "includeTypes";
    private static final String RUNTIME_SCOPE = "runtime";
    private static final String JAR_TYPE = "jar";

    private final InProcessDependencyCopier copier;
    private final MavenExecutor fallback;

    public BarMavenExecutor(InProcessDependencyCopier copier, MavenExecutor fallback) {
        this.copier = copier;
        this.fallback = fallback;
    }

    @Override
    public void execute(File pomFile, List<String> goals, Map<String, String> properties,
            List<String> activeProfiles, Supplier<String> errorMessageBase) throws BuildBarException {
        if (isCopyDependenciesCall(goals, properties)) {
            try {
                File outputDirectory = resolveOutputDirectory(pomFile, properties.get(OUTPUT_DIRECTORY_PROPERTY));
                copier.copyRuntimeDependencies(pomFile, outputDirectory, activeProfiles);
            } catch (MojoExecutionException e) {
                throw new BuildBarException(errorMessageBase.get(), e);
            }
            return;
        }
        fallback.execute(pomFile, goals, properties, activeProfiles, errorMessageBase);
    }

    private boolean isCopyDependenciesCall(List<String> goals, Map<String, String> properties) {
        return COPY_DEPENDENCIES_GOAL.equals(goals)
                && properties.get(OUTPUT_DIRECTORY_PROPERTY) != null
                && RUNTIME_SCOPE.equals(properties.get(INCLUDE_SCOPE_PROPERTY))
                && JAR_TYPE.equals(properties.get(INCLUDE_TYPES_PROPERTY));
    }

    // bonita2bar passes outputDirectory as a path relative to the generated pom's directory
    // (e.g. "./dependencies"), the same way MavenSessionExecutor's fork-based path resolves it
    // by setting the forked process's base directory to pomFile.getParentFile().
    private File resolveOutputDirectory(File pomFile, String outputDirectoryProperty) {
        File outputDirectory = new File(outputDirectoryProperty);
        return outputDirectory.isAbsolute() ? outputDirectory
                : new File(pomFile.getParentFile(), outputDirectoryProperty);
    }

}
