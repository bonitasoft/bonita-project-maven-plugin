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
package org.bonitasoft.plugin.analyze.handler;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;

public abstract class AbstractArtifactAnalyzerHandler implements ArtifactAnalyzerHandler {

    private LocalRepositoryManager localRepositoryManager;

    AbstractArtifactAnalyzerHandler(LocalRepositoryManager localRepositoryManager) {
        this.localRepositoryManager = localRepositoryManager;
    }

    protected org.bonitasoft.plugin.analyze.report.model.Artifact create(Artifact artifact) {
        return org.bonitasoft.plugin.analyze.report.model.Artifact.create(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getBaseVersion() == null ? artifact.getVersion() : artifact.getBaseVersion(),
                artifact.getClassifier(),
                artifactLocalRepositoryFile(artifact));
    }

    protected String artifactLocalRepositoryFile(Artifact artifact) {
        var artifactPath = localRepositoryManager
                .getPathForLocalArtifact(new DefaultArtifact(artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getType(),
                        artifact.getBaseVersion() == null ? artifact.getVersion() : artifact.getBaseVersion()));
        var localRepositoryPath = localRepositoryManager.getRepository().getBasedir().toPath();
        return localRepositoryPath.resolve(artifactPath).toAbsolutePath().toString();
    }

    protected Optional<ZipEntry> findZipEntry(File file, Predicate<ZipEntry> entryPredicate)
            throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            return zipFile.stream().map(ZipEntry.class::cast).filter(entryPredicate).findFirst();
        }
    }
}
