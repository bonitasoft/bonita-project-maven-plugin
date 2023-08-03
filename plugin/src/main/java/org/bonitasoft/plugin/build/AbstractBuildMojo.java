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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.web.designer.config.UiDesignerProperties;
import org.bonitasoft.web.designer.config.UiDesignerPropertiesBuilder;

public abstract class AbstractBuildMojo extends AbstractMojo {

    static {
        // Disable hibernate validator log output from build log
        System.setProperty("org.jboss.logging.provider", "slf4j");
        System.setProperty("org.slf4j.simpleLogger.log.org.hibernate", "off");
    }

    private static final String FRAGMENTS_FOLDER_NAME = "fragmentsFolderName";
    private static final String WIDGETS_FOLDER_NAME = "widgetsFolderName";
    protected static final String PAGES_FOLDER_NAME = "pagesFolderName";
    protected static final String WORKSPACE_PATH = "workspacePath";

    @Parameter(defaultValue = "${project}", required = false, readonly = true)
    protected MavenProject project;

    /**
     * Specify the UI Designer workspace locations.
     * <em>workspacePath</em> : Default to ${project.basedir}
     * <em>pagesFolderName</em> : Default to web_page
     * <em>fragmentsFolderName</em> : Default to web_fragments
     * <em>widgetsFolderName</em> : Default to web_widgets
     */
    @Parameter(property = "uid.workspace")
    private Map<String, String> uidWorkspace;

    /**
     * The build output directory. Default to ${project.build.directory}
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true, readonly = false)
    protected File outputDirectory;

    protected Map<String, String> uidWorkspace() {
        if (uidWorkspace == null) {
            uidWorkspace = new HashMap<>();
        }
        if (project != null) {
            uidWorkspace.putIfAbsent(WORKSPACE_PATH, project.getBasedir().getAbsolutePath());
        }
        uidWorkspace.putIfAbsent(PAGES_FOLDER_NAME, "web_page" /* Studio default name */);
        uidWorkspace.putIfAbsent(WIDGETS_FOLDER_NAME, "web_widgets" /* Studio default name */);
        uidWorkspace.putIfAbsent(FRAGMENTS_FOLDER_NAME, "web_fragments" /* Studio default name */);
        return uidWorkspace;
    }

    protected UiDesignerProperties uidWorkspaceProperties(Path outputDirectory) {
        var workspace = uidWorkspace();
        return new UiDesignerPropertiesBuilder()
                .workspaceUidPath(outputDirectory.resolve("uid"))
                .disableLiveBuild()
                .experimental(false)
                .workspacePath(Paths.get(workspace.get(WORKSPACE_PATH)))
                .pagesFolderName(workspace.get(PAGES_FOLDER_NAME))
                .widgetsFolderName(workspace.get(WIDGETS_FOLDER_NAME))
                .fragmentsFolderName(workspace.get(FRAGMENTS_FOLDER_NAME))
                .build();
    }

}
