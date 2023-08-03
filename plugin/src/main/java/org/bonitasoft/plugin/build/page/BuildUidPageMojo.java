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
package org.bonitasoft.plugin.build.page;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.bonitasoft.plugin.build.AbstractBuildMojo;

/**
 * This mojo builds UI designer pages from sources.
 */
@Mojo(name = "uid-page", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = false)
public class BuildUidPageMojo extends AbstractBuildMojo {

    private static final String[] DEFAULT_EXCLUDES = new String[] { ".metadata" };
    private static final String[] DEFAULT_INCLUDES = new String[] { "*" };

    /**
     * List of pages to include.
     */
    @Parameter(property = "page.includes")
    private String[] includes;

    /**
     * List of pages to exclude.
     */
    @Parameter(property = "page.excludes")
    private String[] excludes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var pagesOutputDirectory = outputDirectory.toPath().resolve("pages");
        pagesOutputDirectory.toFile().mkdirs();

        var artifactBuilder = UidArtifactBuilderFactory
                .create(uidWorkspaceProperties(outputDirectory.toPath()));
        var uidWorkspace = uidWorkspace();
        var pagesFolder = Paths.get(uidWorkspace.get(WORKSPACE_PATH))
                .resolve(uidWorkspace.get(PAGES_FOLDER_NAME));
        try {
            artifactBuilder.buildPages(pagesFolder, selectPages(pagesFolder), pagesOutputDirectory);
        } catch (BuildPageException e) {
            throw new MojoExecutionException(e);
        }
    }

    private String[] selectPages(Path pagesFolder) {
        var fileSetManager = new FileSetManager();
        var pageFileSet = new FileSet();
        pageFileSet.setDirectory(pagesFolder.toFile().getAbsolutePath());
        pageFileSet.setIncludes(Arrays.asList(getIncludes()));
        pageFileSet.setExcludes(Arrays.asList(getExcludes()));
        return fileSetManager.getIncludedDirectories(pageFileSet);
    }

    private String[] getIncludes() {
        if (includes != null && includes.length > 0) {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes() {
        if (excludes != null && excludes.length > 0) {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }
}
