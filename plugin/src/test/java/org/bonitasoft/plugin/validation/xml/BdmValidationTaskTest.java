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
package org.bonitasoft.plugin.validation.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.URL;
import java.nio.file.Paths;

import org.bonitasoft.plugin.validation.ValidateMojo;
import org.bonitasoft.plugin.validation.ValidationException;
import org.junit.jupiter.api.Test;

class BdmValidationTaskTest {

    private static final String TEST_RESOURCES_BDM_DIR = "src/test/resources/validation/bdm";

    private final URL xsdUrl = BdmValidationTaskTest.class.getResource("/validation/bdm/bom.xsd");

    @Test
    void should_validate_valid_bdm() {
        // given
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl,
                Paths.get(TEST_RESOURCES_BDM_DIR, "valid"),
                ValidateMojo.BDM_SOURCE_FILE_REGEX);

        // then
        assertThat(validationTask.getSourceFiles()).hasSize(1);
        assertThatCode(validationTask::validate).doesNotThrowAnyException();
    }

    @Test
    void should_not_validate_invalid_bdm() {
        // given
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl,
                Paths.get(TEST_RESOURCES_BDM_DIR, "invalid"),
                ValidateMojo.BDM_SOURCE_FILE_REGEX);

        // then
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(validationTask::validate)
                .withMessage("File 'bom.xml' is not valid");
    }

    @Test
    void should_not_find_incorrect_bdm_source_file() {
        // given
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl,
                Paths.get(TEST_RESOURCES_BDM_DIR, "incorrect-name"),
                ValidateMojo.BDM_SOURCE_FILE_REGEX);

        // then
        assertThat(validationTask.getSourceFiles()).isEmpty();
        assertThatCode(validationTask::validate).doesNotThrowAnyException();
    }

    @Test
    void should_find_only_one_bdm_source_file() {
        // given
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl,
                Paths.get(TEST_RESOURCES_BDM_DIR, "multiple-files"),
                ValidateMojo.BDM_SOURCE_FILE_REGEX);

        // then
        assertThat(validationTask.getSourceFiles()).hasSize(1);
        assertThatCode(validationTask::validate).doesNotThrowAnyException();
    }
}
