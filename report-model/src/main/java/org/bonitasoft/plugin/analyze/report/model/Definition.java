package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;

@Data
public class Definition {

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

    private GAV gav;

    public static Definition create(DescriptorIdentifier definitionIdentifier,
            String filePath,
            String jarEntry,
            GAV gav) {
        final Definition definition = new Definition();
        definition.setDefinitionId(definitionIdentifier.getId());
        definition.setDefinitionVersion(definitionIdentifier.getVersion());
        definition.setFilePath(filePath);
        definition.setJarEntry(jarEntry);
        definition.setGav(gav);
        return definition;
    }
}
