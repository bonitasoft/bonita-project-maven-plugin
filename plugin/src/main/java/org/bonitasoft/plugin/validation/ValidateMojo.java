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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

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
 * The {@code validate} Mojo is used to execute validation criteria on Bonita artifacts sources located in the current project.
 * <p>
 * It handles the following artifacts:
 * <ul>
 * <li>Applications</li>
 * <li>Profiles</li>
 * <li>BDM</li>
 * <li>BDM Access Control</li>
 * <li>Organizations</li>
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
            executeXmlValidation("Executing XML validation on Applications", applicationXsdPath, applicationSourceDir);

            executeXmlValidation("Executing XML validation on Profiles", profileXsdPath, profileSourceDir);

            executeXmlValidation("Executing XML validation on Organizations", organizationXsdPath,
                    organizationSourceDir, ORGANIZATION_SOURCE_FILE_REGEX);

            executeBdmValidation();

            executeUidValidation();
        } catch (ValidationException e) {
            throw new MojoFailureException("Project validation failed with the following error: " + e.getMessage(), e);
        }
    }

    /**
     * Call {@link XmlValidationTask#validate()} with given arguments if the directory {@code artifactsSourceDir} is
     * valid.
     *
     * @param logMessage a message to log before executing the validation
     * @param xsdPath path to the XSD schema
     * @param artifactsSourceDir path to the directory containing XML source files to validate
     * @throws ValidationException if validation criteria are not met
     */
    private void executeXmlValidation(String logMessage, String xsdPath, String artifactsSourceDir)
            throws ValidationException {
        executeXmlValidation(logMessage, xsdPath, artifactsSourceDir, null);
    }

    /**
     * Call {@link XmlValidationTask#validate()} with given arguments if the directory {@code artifactsSourceDir} is
     * valid.
     *
     * @param logMessage a message to log before executing the validation
     * @param xsdPath path to the XSD schema
     * @param artifactsSourceDir path to the directory containing XML source files to validate
     * @param sourceFileRegex regex used to filter source files
     * @throws ValidationException if validation criteria are not met
     */
    private void executeXmlValidation(String logMessage, String xsdPath, String artifactsSourceDir,
            String sourceFileRegex) throws ValidationException {
        Path sourceDir = project.getBasedir().toPath().resolve(artifactsSourceDir);
        if (isArtifactsSourceDirValid(sourceDir)) {
            log.info(logMessage);
            new XmlValidationTask(ValidateMojo.class.getResource(xsdPath), sourceDir, sourceFileRegex).validate();
        }
    }

    /**
     * Execute XML validation for BDM and BDM Access Control artifacts.
     *
     * @throws ValidationException if validation criteria are not met
     */
    private void executeBdmValidation() throws ValidationException {
        // because xml files are located at root level of 'bdm' folder, we check that we are under this folder before
        // instantiate unnecessary xml validators
        String baseDirName = project.getBasedir().getName();
        if (!BDM_FOLDER_NAME.equals(baseDirName)) {
            log.debug("Current project base dir [{}] is not the '{}' directory, skip BDM validation", baseDirName,
                    BDM_FOLDER_NAME);
            return;
        }

        executeXmlValidation("Executing XML validation on BDM", bdmXsdPath, bdmSourceDir, BDM_SOURCE_FILE_REGEX);

        executeXmlValidation("Executing XML validation on BDM Access Control", bdmAccessControlXsdPath,
                bdmAccessControlSourceDir, BDM_ACCESS_CONTROL_SOURCE_FILE_REGEX);
    }

    /**
     * Execute validation for the following UID artifacts: Pages, Fragments, Widgets.
     *
     * @throws ValidationException if validation criteria are not met
     */
    private void executeUidValidation() throws ValidationException {
        String baseDirName = project.getBasedir().getName();
        if (!APP_FOLDER_NAME.equals(baseDirName)) {
            log.debug("Current project base dir [{}] is not the '{}' directory, skip UID validation", baseDirName,
                    APP_FOLDER_NAME);
            return;
        }

        // init the artifact builder used to validate UID artifacts
        UiDesignerProperties uidWorkspaceProperties = uidWorkspaceProperties(outputDirectory.toPath());
        ArtifactBuilder uidArtifactBuilder = new ArtifactBuilderFactory(uidWorkspaceProperties).create();

        Path pagesDir = uidWorkspaceProperties.getWorkspace().getPages().getDir();
        if (isArtifactsSourceDirValid(pagesDir)) {
            log.info("Executing UID validation on Pages");
            new PageUidValidationTask(uidArtifactBuilder, pagesDir).validate();
        }

        Path fragmentsDir = uidWorkspaceProperties.getWorkspace().getFragments().getDir();
        if (isArtifactsSourceDirValid(fragmentsDir)) {
            log.info("Executing UID validation on Fragments");
            new FragmentUidValidationTask(uidArtifactBuilder, fragmentsDir).validate();
        }

        Path widgetsDir = uidWorkspaceProperties.getWorkspace().getWidgets().getDir();
        if (isArtifactsSourceDirValid(widgetsDir)) {
            log.info("Executing UID validation on Widgets");
            new WidgetUidValidationTask(uidArtifactBuilder, widgetsDir).validate();
        }
    }

    /**
     * An artifacts source directory is valid if it exists and it is not empty.
     *
     * @param artifactsSourceDir path to the directory to check
     * @return {@code true} if the artifacts source directory is valid
     */
    private boolean isArtifactsSourceDirValid(Path artifactsSourceDir) {
        if (Files.exists(artifactsSourceDir) && Files.isDirectory(artifactsSourceDir)) {
            // check if directory is not empty
            try (Stream<Path> entries = Files.list(artifactsSourceDir)) {
                return entries.findAny().isPresent();
            } catch (IOException e) {
                throw new ValidationErrorException(
                        "An error occurred when listing files in the directory " + artifactsSourceDir, e);
            }
        }
        log.debug("Artifacts source directory [{}] does not exist or is not a directory", artifactsSourceDir);
        return false;
    }

}
