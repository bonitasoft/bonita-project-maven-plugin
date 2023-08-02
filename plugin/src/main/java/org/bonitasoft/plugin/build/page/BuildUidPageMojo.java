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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.bonitasoft.web.designer.config.UiDesignerProperties;
import org.bonitasoft.web.designer.config.UiDesignerPropertiesBuilder;

@Mojo(name = "build-page", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresProject = false)
public class BuildUidPageMojo extends AbstractMojo {

    private static final String FRAGMENTS_FOLDER_NAME = "fragmentsFolderName";
    private static final String WIDGETS_FOLDER_NAME = "widgetsFolderName";
    private static final String PAGES_FOLDER_NAME = "pagesFolderName";
    private static final String WORKSPACE_PATH = "workspacePath";

    static {
        java.util.logging.Logger.getLogger("org.hibernate").setLevel(Level.OFF);
    }

    private static final String[] DEFAULT_EXCLUDES = new String[] { ".metadata" };
    private static final String[] DEFAULT_INCLUDES = new String[] { "*" };

    @Parameter(defaultValue = "${project}", required = false, readonly = true)
    private MavenProject project;

    @Parameter(property = "uid.workspace")
    private Map<String, String> uidWorkspace;

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

    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true, readonly = false)
    private File outputDirectory;

    private UidArtifactBuilder.Factory artifactBuilderFactory;

    @Inject
    public BuildUidPageMojo(UidArtifactBuilder.Factory artifactBuilderFactory) {
        this.artifactBuilderFactory = artifactBuilderFactory;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (uidWorkspace == null) {
            uidWorkspace = new HashMap<>();
        }
        if (project != null) {
            uidWorkspace.putIfAbsent(WORKSPACE_PATH, project.getBasedir().getAbsolutePath());
        }
        uidWorkspace.putIfAbsent(PAGES_FOLDER_NAME, "web_page" /* Studio default name */);
        uidWorkspace.putIfAbsent(WIDGETS_FOLDER_NAME, "web_widgets" /* Studio default name */);
        uidWorkspace.putIfAbsent(FRAGMENTS_FOLDER_NAME, "web_fragments" /* Studio default name */);

        var pagesOutputDirectory = outputDirectory.toPath().resolve("pages");
        pagesOutputDirectory.toFile().mkdirs();

        var fileSetManager = new FileSetManager();
        var pageFileSet = new FileSet();
        pageFileSet.setDirectory(Paths.get(uidWorkspace.get(WORKSPACE_PATH))
                .resolve(uidWorkspace.get(PAGES_FOLDER_NAME)).toFile().getAbsolutePath());
        pageFileSet.setIncludes(Arrays.asList(getIncludes()));
        pageFileSet.setExcludes(Arrays.asList(getExcludes()));

        var artifactBuilder = artifactBuilderFactory
                .create(uidWorkspaceProperties(uidWorkspace, outputDirectory.toPath()), pagesOutputDirectory);
        try {
            artifactBuilder.buildPages(fileSetManager.getIncludedDirectories(pageFileSet));
        } catch (BuildPageException e) {
            throw new MojoExecutionException(e);
        }
    }

    private UiDesignerProperties uidWorkspaceProperties(Map<String, String> uidWorkspace, Path outputDirectory) {
        return new UiDesignerPropertiesBuilder()
                .workspaceUidPath(outputDirectory.resolve("uid"))
                .disableLiveBuild()
                .workspacePath(Paths.get(uidWorkspace.get(WORKSPACE_PATH)))
                .pagesFolderName(uidWorkspace.get(PAGES_FOLDER_NAME))
                .widgetsFolderName(uidWorkspace.get(WIDGETS_FOLDER_NAME))
                .fragmentsFolderName(uidWorkspace.get(FRAGMENTS_FOLDER_NAME))
                .build();
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
