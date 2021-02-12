package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Definition extends GAV {

    private String definitionId;

    private String definitionVersion;

    /**
     * Jar file containing the definition descriptor file
     */
    private String filePath;

    /**
     * Definition descriptor file entry path in the jar file
     */
    private String jarEntry;

    public static Definition create(DescriptorIdentifier definitionIdentifier,
            String filePath,
            String jarEntry,
            String groupId,
            String artifactID,
            String version) {
        final Definition definition = new Definition();
        definition.setDefinitionId(definitionIdentifier.getId());
        definition.setDefinitionVersion(definitionIdentifier.getVersion());
        definition.setFilePath(filePath);
        definition.setJarEntry(jarEntry);
        definition.setGroupId(groupId);
        definition.setArtifactId(artifactID);
        definition.setVersion(version);
        return definition;
    }
}
