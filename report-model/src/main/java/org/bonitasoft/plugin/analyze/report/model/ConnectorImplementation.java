package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("CONNECTOR")
public class ConnectorImplementation extends Implementation {

    public static ConnectorImplementation create(String className,
            DescriptorIdentifier definitionIdentifier,
            DescriptorIdentifier implementationIdentifier,
            String filePath,
            String jarEntry,
            String groupId,
            String artifactID,
            String version) {
        return Implementation.create(className,
                definitionIdentifier,
                implementationIdentifier,
                filePath,
                jarEntry,
                ConnectorImplementation.class,
                groupId,
                artifactID,
                version);
    }

}
