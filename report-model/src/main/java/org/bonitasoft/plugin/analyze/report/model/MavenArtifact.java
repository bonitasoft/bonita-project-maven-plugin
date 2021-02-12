package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;

@Data
public class MavenArtifact {

    private String groupId;

    private String artifactId;

    private String version;

    private String classifier;

    private String type;

    public static MavenArtifact create(String groupId, String artifactId, String version, String classifier, String type) {
        MavenArtifact artifact = new MavenArtifact();
        artifact.setGroupId(groupId);
        artifact.setArtifactId(artifactId);
        artifact.setVersion(version);
        artifact.setClassifier(classifier);
        artifact.setType(type);
        return artifact;
    }

}
