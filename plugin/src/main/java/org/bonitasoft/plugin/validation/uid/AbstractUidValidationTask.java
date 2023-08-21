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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bonitasoft.plugin.validation.ValidationErrorException;
import org.bonitasoft.plugin.validation.ValidationException;
import org.bonitasoft.plugin.validation.ValidationTask;
import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.model.MigrationStatusReport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract class used to check that UID artifacts located in a directory are valid.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractUidValidationTask implements ValidationTask {

    /**
     * Regex representing directories that are not UID artifacts
     */
    private static final String UID_DIRECTORY_EXCLUDE_REGEX = "^(?:pb.*|.metadata)$";

    /**
     * Artifact builder required to check if a UID artifact is valid
     */
    protected final ArtifactBuilder artifactBuilder;

    /**
     * Directory where UID artifacts should be stored
     */
    protected final Path artifactsSourceDir;

    @Override
    public void validate() throws ValidationException {
        for (String uidArtifact : getUidArtifacts()) {
            log.debug("Executing validation on UID artifact [{}]", uidArtifact);
            if (!getArtifactStatus(uidArtifact).isCompatible()) {
                throw new ValidationException("UID artifact '" + uidArtifact + "' is not valid");
            }
            log.info("UID artifact '{}' is valid", uidArtifact);
        }
    }

    protected abstract MigrationStatusReport getArtifactStatus(String artifactId);

    protected List<String> getUidArtifacts() {
        try (Stream<Path> sourcePaths = Files.list(artifactsSourceDir)) {
            var sourceFiles = sourcePaths
                    .filter(AbstractUidValidationTask::isUidArtifact)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            log.debug("Found [{}] UID artifacts in directory [{}]", sourceFiles.size(), artifactsSourceDir);
            return sourceFiles;
        } catch (IOException e) {
            throw new ValidationErrorException("Failed to list UID artifacts in directory " + artifactsSourceDir, e);
        }
    }

    /**
     * Check if the given directory represents a UID artifact.
     * <p>
     * The directory is a UID artifact if its name does not
     * match the regex {@link AbstractUidValidationTask#UID_DIRECTORY_EXCLUDE_REGEX} and if it contains a json file
     * matching exactly the directory name.
     * <p>
     * For example the directory {@code fooBar} must contain the json file {@code fooBar.json} to be identified as a UID
     * artifacts.
     *
     * @param directory path of the directory to check
     * @return {@code true} if the directory matches criteria described above
     * @throws ValidationErrorException if an error occurred when parsing the given directory
     */
    protected static boolean isUidArtifact(Path directory) throws ValidationErrorException {
        var directoryName = directory.getFileName().toString();
        // check if the directory exists and does not match directories to exclude
        if (Files.exists(directory) && Files.isDirectory(directory)
                && !directoryName.matches(UID_DIRECTORY_EXCLUDE_REGEX)) {
            try (Stream<Path> directoryEntries = Files.list(directory)) {
                // the directory is an UID artifact if it contains a json with the exact same name
                var expectedJson = directoryName + ".json";
                return directoryEntries.filter(Files::isRegularFile)
                        .filter(file -> expectedJson.equals(file.getFileName().toString()))
                        .count() == 1;
            } catch (IOException e) {
                throw new ValidationErrorException("An error occurred when listing files in directory " + directory, e);
            }
        }
        return false;
    }
}
