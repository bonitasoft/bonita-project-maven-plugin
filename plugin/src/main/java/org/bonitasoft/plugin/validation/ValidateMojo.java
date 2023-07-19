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

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
@Mojo(name = "validate", defaultPhase = LifecyclePhase.NONE)
public class ValidateMojo extends AbstractMojo {

    public static final String BDM_SOURCE_FILE_REGEX = "^bom.xml$";
    public static final String BDM_ACCESS_CONTROL_SOURCE_FILE_REGEX = "^bdm_access_control.xml$";
    public static final String ORGANIZATION_SOURCE_FILE_REGEX = "^.*\\.organization$";

    @Parameter(defaultValue = "${project.basedir}", required = true, readonly = true)
    private File projectBaseDir;

    @Parameter(property = "validation.application.xsdPath", defaultValue = "/application.xsd")
    private String applicationXsdPath;
    @Parameter(property = "validation.application.sourceDir", defaultValue = "app/applications/")
    private String applicationSourceDir;

    @Parameter(property = "validation.profile.xsdPath", defaultValue = "/profiles.xsd")
    private String profileXsdPath;
    @Parameter(property = "validation.profile.sourceDir", defaultValue = "app/profiles/")
    private String profileSourceDir;

    @Parameter(property = "validation.bdm.xsdPath", defaultValue = "/bom.xsd")
    private String bdmXsdPath;
    @Parameter(property = "validation.bdm.sourceDir", defaultValue = "bdm/")
    private String bdmSourceDir;

    @Parameter(property = "validation.bdmAccessControl.xsdPath", defaultValue = "/bdm-access-control.xsd")
    private String bdmAccessControlXsdPath;
    @Parameter(property = "validation.bdmAccessControl.sourceDir", defaultValue = "bdm/")
    private String bdmAccessControlSourceDir;

    @Parameter(property = "validation.organization.xsdPath", defaultValue = "/organization.xsd")
    private String organizationXsdPath;
    @Parameter(property = "validation.organization.sourceDir", defaultValue = "app/organizations/")
    private String organizationSourceDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            log.info("Executing XML validation on Applications");
            executeXmlValidation(applicationXsdPath, applicationSourceDir);

            log.info("Executing XML validation on Profiles");
            executeXmlValidation(profileXsdPath, profileSourceDir);

            log.info("Executing XML validation on BDM");
            executeXmlValidation(bdmXsdPath, bdmSourceDir, BDM_SOURCE_FILE_REGEX);

            log.info("Executing XML validation on BDM Access Control");
            // FIXME remove this try-catch after extracting BDM Access Control model from bonita-common-sp
            try {
                executeXmlValidation(bdmAccessControlXsdPath, bdmAccessControlSourceDir,
                        BDM_ACCESS_CONTROL_SOURCE_FILE_REGEX);
            } catch (ValidationErrorException e) {
                log.warn(bdmAccessControlXsdPath + " is not available in the classpath", e);
            }

            log.info("Executing XML validation on Organizations");
            // FIXME remove this try-catch after extracting Organization model from bonita-process-engine
            try {
                executeXmlValidation(organizationXsdPath, organizationSourceDir, ORGANIZATION_SOURCE_FILE_REGEX);
            } catch (ValidationErrorException e) {
                log.warn(organizationXsdPath + " is not available in the classpath", e);
            }

            // TODO uid validation
        } catch (ValidationException e) {
            throw new MojoFailureException("Project validation failed with the following error: " + e.getMessage(), e);
        }
    }

    /**
     * Call {@link XmlValidationTask#validate()} with given arguments.
     * 
     * @param xsdPath path to the XSD schema
     * @param sourceDir path to the directory containing XML source files to validate
     * @throws ValidationException if validation criteria are not met
     */
    private void executeXmlValidation(String xsdPath, String sourceDir) throws ValidationException {
        new XmlValidationTask(ValidateMojo.class.getResource(xsdPath), projectBaseDir.toPath().resolve(sourceDir))
                .validate();
    }

    /**
     * Call {@link XmlValidationTask#validate()} with given arguments.
     *
     * @param xsdPath path to the XSD schema
     * @param sourceDir path to the directory containing XML source files to validate
     * @param sourceFileRegex regex used to filter source files
     * @throws ValidationException if validation criteria are not met
     */
    private void executeXmlValidation(String xsdPath, String sourceDir, String sourceFileRegex)
            throws ValidationException {
        new XmlValidationTask(ValidateMojo.class.getResource(xsdPath), projectBaseDir.toPath().resolve(sourceDir),
                sourceFileRegex).validate();
    }

}
