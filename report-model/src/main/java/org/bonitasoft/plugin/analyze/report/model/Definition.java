package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;

@Data
public class Definition {

    private String definitionId;

    private String definitionVersion;

    /**
     * Artifact containing the definition descriptor file
     */
    private Artifact artifact;

    /**
     * Definition descriptor file entry path in the jar file
     */
    private String jarEntry;

    public static Definition create(DescriptorIdentifier definitionIdentifier,
            Artifact artifact,
            String jarEntry) {
        final Definition definition = new Definition();
        definition.setDefinitionId(definitionIdentifier.getId());
        definition.setDefinitionVersion(definitionIdentifier.getVersion());
        definition.setArtifact(artifact);
        definition.setJarEntry(jarEntry);
        return definition;
    }
}
