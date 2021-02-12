package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("APIEXTENSION")
public class RestAPIExtension extends CustomPage {

    public static RestAPIExtension create(String name, String displayName, String description, String filePath,
            String groupId, String artifactID, String version) {
        return CustomPage.create(name, displayName, description, filePath, RestAPIExtension.class, groupId, artifactID,
                version);
    }

}
