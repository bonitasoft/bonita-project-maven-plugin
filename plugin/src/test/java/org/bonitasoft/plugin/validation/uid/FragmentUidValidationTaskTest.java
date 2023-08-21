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
package org.bonitasoft.plugin.validation.uid;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;

import org.bonitasoft.plugin.validation.ValidationException;
import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.model.MigrationStatusReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FragmentUidValidationTaskTest {

    private static final String TEST_RESOURCES_FRAGMENTS_DIR = "src/test/resources/validation/fragments";
    private static final String MOCKED_UID_FRAGMENT_1 = "mockedUidFragment1";
    private static final String MOCKED_UID_FRAGMENT_2 = "mockedUidFragment2";

    @Mock
    private ArtifactBuilder artifactBuilder;
    private final MigrationStatusReport validStatus = new MigrationStatusReport(true, true);
    private final MigrationStatusReport invalidStatus = new MigrationStatusReport(false, false);

    @Test
    void should_validate_valid_uid_fragments() {
        // given
        var validationTask = new FragmentUidValidationTask(artifactBuilder, Paths.get(TEST_RESOURCES_FRAGMENTS_DIR));
        when(artifactBuilder.getFragmentStatus(MOCKED_UID_FRAGMENT_1)).thenReturn(validStatus);
        when(artifactBuilder.getFragmentStatus(MOCKED_UID_FRAGMENT_2)).thenReturn(validStatus);

        // then
        assertThat(validationTask.getUidArtifacts()).hasSize(2);
        assertThatCode(validationTask::validate).doesNotThrowAnyException();
    }

    @Test
    void should_not_validate_invalid_uid_fragments() {
        // given
        var validationTask = new FragmentUidValidationTask(artifactBuilder, Paths.get(TEST_RESOURCES_FRAGMENTS_DIR));
        when(artifactBuilder.getFragmentStatus(MOCKED_UID_FRAGMENT_1)).thenReturn(validStatus);
        when(artifactBuilder.getFragmentStatus(MOCKED_UID_FRAGMENT_2)).thenReturn(invalidStatus);

        // then
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(validationTask::validate)
                .withMessage("UID artifact '" + MOCKED_UID_FRAGMENT_2 + "' is not valid");
    }

}
