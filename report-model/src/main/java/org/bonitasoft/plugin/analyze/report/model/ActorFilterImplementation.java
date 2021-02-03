package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ACTOR_FILTER")
public class ActorFilterImplementation extends Implementation {

    public static ActorFilterImplementation create(String className,
            DescriptorIdentifier definitionIdentifier,
            DescriptorIdentifier implementationIdentifier,
            String filePath,
            String jarEntry) {
        return Implementation.create(className,
                definitionIdentifier,
                implementationIdentifier,
                filePath,
                jarEntry,
                ActorFilterImplementation.class);
    }

}
