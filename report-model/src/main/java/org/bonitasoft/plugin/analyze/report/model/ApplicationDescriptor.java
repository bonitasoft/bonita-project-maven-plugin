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
package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;

@Data
public class ApplicationDescriptor {

    private String displayName;
    private String version;
    private String description;
    private String profile;
    private String appToken;
    private Artifact artifact;

    public static ApplicationDescriptor create(String displayName,
            String version,
            String description,
            String profile,
            String appToken,
            Artifact artifact) {
        var descriptor = new ApplicationDescriptor();
        descriptor.setDisplayName(displayName);
        descriptor.setDescription(description);
        descriptor.setVersion(version);
        descriptor.setProfile(profile);
        descriptor.setAppToken(appToken);
        descriptor.setArtifact(artifact);
        return descriptor;
    }

}
