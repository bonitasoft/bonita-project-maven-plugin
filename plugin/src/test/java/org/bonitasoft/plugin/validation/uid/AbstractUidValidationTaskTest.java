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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.model.ArtifactStatusReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractUidValidationTaskTest {

    private static final String TEST_RESOURCES_UID_DIR = "src/test/resources/validation/uid";

    @Mock
    private ArtifactBuilder artifactBuilder;

    @Test
    void should_be_uid_artifact_with_valid_directory() {
        // given
        Path artifactDir = Paths.get(TEST_RESOURCES_UID_DIR, "valid-artifact", "foobar");

        // then
        assertThat(AbstractUidValidationTask.isUidArtifact(artifactDir)).isTrue();
    }

    @Test
    void should_not_be_uid_artifact_with_invalid_directory() {
        assertUidArtifactIsFalse("invalid-artifacts", "foobar");
    }

    @Test
    void should_not_be_uid_artifact_with_non_existing_directory() {
        assertUidArtifactIsFalse("invalid-artifacts", "non-existing-artifact");
    }

    @Test
    void should_not_be_uid_artifact_with_regular_file() {
        assertUidArtifactIsFalse("invalid-artifacts", "regular-file");
    }

    @Test
    void should_not_be_uid_artifact_with_metadata_directory() {
        assertUidArtifactIsFalse("invalid-artifacts", ".metadata");
    }

    @Test
    void should_not_be_uid_artifact_with_pb_directory() {
        assertUidArtifactIsFalse("invalid-artifacts", "pbText");
    }

    @Test
    void should_get_one_valid_uid_artifact() {
        assertGetUidArtifactsHasExpectedSize(1, "valid-artifact");
    }

    @Test
    void should_get_multiple_valid_uid_artifacts() {
        assertGetUidArtifactsHasExpectedSize(3, "multiple-artifacts");
    }

    @Test
    void should_get_no_uid_artifact_with_invalid_directories() {
        assertGetUidArtifactsHasExpectedSize(0, "invalid-artifacts");
    }

    @Test
    void should_get_no_uid_artifact_with_empty_directory() {
        assertGetUidArtifactsHasExpectedSize(0, "no-artifact");
    }

    @Test
    void should_get_no_uid_artifact_with_non_existing_directory() {
        assertGetUidArtifactsHasExpectedSize(0, "non-existing-directory");
    }

    private void assertUidArtifactIsFalse(String... directories) {
        // given
        Path artifactDir = Paths.get(TEST_RESOURCES_UID_DIR, directories);

        // then
        assertThat(AbstractUidValidationTask.isUidArtifact(artifactDir)).isFalse();
    }

    private void assertGetUidArtifactsHasExpectedSize(int expectedSize, String directory) {
        // given
        Path artifactsSourceDir = Paths.get(TEST_RESOURCES_UID_DIR, directory);
        AbstractUidValidationTask validationTask = new TestUidValidationTask(artifactBuilder, artifactsSourceDir);

        // then
        assertThat(validationTask.getUidArtifacts()).hasSize(expectedSize);
    }

    private static class TestUidValidationTask extends AbstractUidValidationTask {

        public TestUidValidationTask(ArtifactBuilder artifactBuilder, Path artifactsSourceDir) {
            super(artifactBuilder, artifactsSourceDir);
        }

        @Override
        protected String getTaskName() {
            return "Test UID Validation Task";
        }

        @Override
        protected ArtifactStatusReport getArtifactStatus(String artifactId) {
            return null;
        }
    }
}
