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
     * Jar file containing the implementation descriptor file
     */
    private String filePath;

    /**
     * Implementation descriptor file entry path in the jar file
     */
    private String jarEntry;

    private MavenArtifact mavenArtifact;

    public static <T extends Implementation> T create(String className,
            DescriptorIdentifier definitionIdentifier,
            DescriptorIdentifier implementationIdentifier,
            String filePath,
            String jarEntry,
            Class<T> type,
            MavenArtifact mavenArtifact) {
        try {
            T implementation = type.getDeclaredConstructor().newInstance();
            implementation.setClassName(className);
            implementation.setDefinitionId(definitionIdentifier.getId());
            implementation.setDefinitionVersion(definitionIdentifier.getVersion());
            implementation.setImplementationId(implementationIdentifier.getId());
            implementation.setImplementationVersion(implementationIdentifier.getVersion());
            implementation.setFilePath(filePath);
            implementation.setJarEntry(jarEntry);
            implementation.setMavenArtifact(mavenArtifact);
            return implementation;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create a new instance of class: " + type.getName(), e);
        }
    }

}
