package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("FORM")
public class Form extends CustomPage {

    public static Form create(String name, String displayName, String description, String filePath, MavenArtifact mavenArtifact) {
        return CustomPage.create(name, displayName, description, filePath, Form.class, mavenArtifact);
    }

}
