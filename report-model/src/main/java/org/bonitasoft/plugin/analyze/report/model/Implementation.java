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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = ConnectorImplementation.class, name = "CONNECTOR"),
        @Type(value = ActorFilterImplementation.class, name = "ACTOR_FILTER")
})
public abstract class Implementation {

    private String className;

    private String implementationId;

    private String implementationVersion;

    private String definitionId;

    private String definitionVersion;

    /**
     * Artifact containing the implementation descriptor file
     */
    private Artifact artifact;

    /**
     * Implementation descriptor file entry path in the jar file
     */
    private String jarEntry;

    public static <T extends Implementation> T create(String className,
            DescriptorIdentifier definitionIdentifier,
            DescriptorIdentifier implementationIdentifier,
            Artifact artifact,
            String jarEntry,
            Class<T> type) {
        try {
            T implementation = type.getDeclaredConstructor().newInstance();
            implementation.setClassName(className);
            implementation.setDefinitionId(definitionIdentifier.getId());
            implementation.setDefinitionVersion(definitionIdentifier.getVersion());
            implementation.setImplementationId(implementationIdentifier.getId());
            implementation.setImplementationVersion(implementationIdentifier.getVersion());
            implementation.setArtifact(artifact);
            implementation.setJarEntry(jarEntry);
            return implementation;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create a new instance of class: " + type.getName(), e);
        }
    }

}
