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

import java.nio.file.Path;

import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.model.ArtifactStatusReport;

/**
 * Class used to check that UID pages are valid.
 */
public class PageUidValidationTask extends AbstractUidValidationTask {

    public PageUidValidationTask(ArtifactBuilder artifactBuilder, Path artifactsSourceDir) {
        super(artifactBuilder, artifactsSourceDir);
    }

    @Override
    protected String getTaskName() {
        return "UID Pages validation";
    }

    @Override
    protected ArtifactStatusReport getArtifactStatus(String artifactId) {
        return artifactBuilder.getPageStatus(artifactId);
    }
}
