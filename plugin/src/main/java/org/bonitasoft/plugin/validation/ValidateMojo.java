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
package org.bonitasoft.plugin.validation;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.bonitasoft.plugin.AbstractBuildMojo;
import org.bonitasoft.plugin.validation.uid.FragmentUidValidationTask;
import org.bonitasoft.plugin.validation.uid.PageUidValidationTask;
import org.bonitasoft.plugin.validation.uid.WidgetUidValidationTask;
import org.bonitasoft.plugin.validation.xml.XmlValidationTask;
import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.ArtifactBuilderFactory;
import org.bonitasoft.web.designer.config.UiDesignerProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * The {@code validate} Mojo is used to execute validation criteria on Bonita artifacts sources located in the current
 * project.
 * <p>
 * It handles the following artifacts:
 * <ul>
 * <li>Applications</li>
 * <li>Profiles</li>
 * <li>BDM</li>
 * <li>BDM Access Control</li>
 * <li>Organizations</li>
 * <li>UID Pages</li>
 * <li>UID Fragments</li>
 * <li>UID Widgets</li>
 * </ul>
 */
@Slf4j
@Mojo(name = "validate", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateMojo extends AbstractBuildMojo {

    public static final String BDM_SOURCE_FILE_REGEX = "^bom.xml$";
    public static final String BDM_ACCESS_CONTROL_SOURCE_FILE_REGEX = "^bdm_access_control.xml$";
    public static final String ORGANIZATION_SOURCE_FILE_REGEX = "^.*\\.organization$";

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            executeXmlValidation("XML validation on Applications", applicationXsdPath, applicationSourceDir);

            executeXmlValidation("XML validation on Profiles", profileXsdPath, profileSourceDir);

            executeXmlValidation("XML validation on Organizations", organizationXsdPath, organizationSourceDir,
                    ORGANIZATION_SOURCE_FILE_REGEX);

            executeXmlValidation("XML validation on BDM", bdmXsdPath, bdmSourceDir, BDM_SOURCE_FILE_REGEX);

            executeXmlValidation("XML validation on BDM Access Control", bdmAccessControlXsdPath,
                    bdmAccessControlSourceDir, BDM_ACCESS_CONTROL_SOURCE_FILE_REGEX);

            executeUidValidation();
        } catch (ValidationException e) {
            throw new MojoFailureException("Project validation failed with the following error: " + e.getMessage(), e);
        }
    }

    /**
     * Call {@link XmlValidationTask#validate()} with given arguments.
     *
     * @param taskName name of the validation task
     * @param xsdPath path to the XSD schema
     * @param artifactsSourceDir path to the directory containing XML source files to validate
     * @throws ValidationException if validation criteria are not met
     */
    private void executeXmlValidation(String taskName, String xsdPath, String artifactsSourceDir)
            throws ValidationException {
        executeXmlValidation(taskName, xsdPath, artifactsSourceDir, null);
    }

    /**
     * Call {@link XmlValidationTask#validate()} with given arguments.
     *
     * @param taskName name of the validation task
     * @param xsdPath path to the XSD schema
     * @param artifactsSourceDir path to the directory containing XML source files to validate
     * @param sourceFileRegex regex used to filter source files
     * @throws ValidationException if validation criteria are not met
     */
    private void executeXmlValidation(String taskName, String xsdPath, String artifactsSourceDir,
            String sourceFileRegex) throws ValidationException {
        new XmlValidationTask(taskName, ValidateMojo.class.getResource(xsdPath),
                project.getBasedir().toPath().resolve(artifactsSourceDir), sourceFileRegex).validate();
    }

    /**
     * Execute validation for the following UID artifacts: Pages, Fragments, Widgets.
     *
     * @throws ValidationException if validation criteria are not met
     */
    private void executeUidValidation() throws ValidationException {
        // check that the current base dir is the 'app' submodule to prevent the initialization of the UID artifact
        // builder for every project submodules
        String baseDirName = project.getBasedir().getName();
        if (!APP_FOLDER_NAME.equals(baseDirName)) {
            log.debug("Current project base dir [{}] is not the '{}' directory, skip UID validation", baseDirName,
                    APP_FOLDER_NAME);
            return;
        }

        // init the artifact builder used to validate UID artifacts
        UiDesignerProperties uidWorkspaceProperties = uidWorkspaceProperties(outputDirectory.toPath());
        ArtifactBuilder uidArtifactBuilder = new ArtifactBuilderFactory(uidWorkspaceProperties).create();

        new PageUidValidationTask(uidArtifactBuilder, uidWorkspaceProperties.getWorkspace().getPages().getDir())
                .validate();

        new FragmentUidValidationTask(uidArtifactBuilder, uidWorkspaceProperties.getWorkspace().getFragments().getDir())
                .validate();

        new WidgetUidValidationTask(uidArtifactBuilder, uidWorkspaceProperties.getWorkspace().getWidgets().getDir())
                .validate();
    }

}
