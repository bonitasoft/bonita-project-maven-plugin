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

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.nio.file.Paths;

import org.bonitasoft.plugin.validation.ValidationErrorException;
import org.junit.jupiter.api.Test;

class XmlValidationTaskTest {

    private static final String TEST_RESOURCES_VALIDATION_DIR = "src/test/resources/validation/";

    @Test
    void should_throw_exception_if_non_existing_xsd() {
        // given
        URL xsdUrl = XmlValidationTask.class.getResource("foo.bar");
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl, null);

        // then
        assertThatExceptionOfType(ValidationErrorException.class)
                .isThrownBy(validationTask::initValidator)
                .withMessage("[" + XmlValidationTask.DEFAULT_TASK_NAME + "] Failed to parse XSD with URL null");
    }

    @Test
    void should_throw_exception_if_invalid_xsd() {
        // given
        URL xsdUrl = XmlValidationTask.class.getResource("/validation/invalid-xsd.xsd");
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl, null);

        // then
        assertThatExceptionOfType(ValidationErrorException.class)
                .isThrownBy(validationTask::initValidator)
                .withMessage("[" + XmlValidationTask.DEFAULT_TASK_NAME + "] Failed to parse XSD with URL " + xsdUrl);
    }

    @Test
    void should_ignore_no_source_file() {
        // given
        URL xsdUrl = XmlValidationTask.class.getResource("/validation/empty-xsd.xsd");
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl,
                Paths.get(TEST_RESOURCES_VALIDATION_DIR, "no-source-file"));

        // then
        assertThat(validationTask.getSourceFiles()).isEmpty();
        assertThatCode(validationTask::validate).doesNotThrowAnyException();
    }

    @Test
    void should_ignore_non_existing_source_dir() {
        // given
        URL xsdUrl = XmlValidationTask.class.getResource("/validation/empty-xsd.xsd");
        XmlValidationTask validationTask = new XmlValidationTask(xsdUrl,
                Paths.get(TEST_RESOURCES_VALIDATION_DIR, "not-exist"));

        // then
        assertThat(validationTask.getSourceFiles()).isEmpty();
        assertThatCode(validationTask::validate).doesNotThrowAnyException();
    }
}
