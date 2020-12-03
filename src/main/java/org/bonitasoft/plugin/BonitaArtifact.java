package org.bonitasoft.plugin;

import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.w3c.dom.Document;

public class BonitaArtifact {

    public abstract static class CustomPage {

        private final String name;
        private final String displayName;
        private final String description;
        private final Artifact artifact;

        private CustomPage(String name, String displayName, String description, Artifact artifact) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.artifact = artifact;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public Artifact getArtifact() {
            return artifact;
        }

        @Override
        public String toString() {
            return displayName + " in " + artifact;
        }

    }

    public static class RestAPIExtension extends CustomPage {

        private static final String DESCRIPTION_PROPERTY = "description";
        private static final String DISPLAY_NAME_PROPERTY = "displayName";
        private static final String NAME_PROPERTY = "name";

        public static RestAPIExtension create(Properties properties, Artifact artifact) {
            String name = properties.getProperty(NAME_PROPERTY);
            String displayName = properties.getProperty(DISPLAY_NAME_PROPERTY);
            String description = properties.getProperty(DESCRIPTION_PROPERTY);
            return new RestAPIExtension(name, displayName, description, artifact);
        }

        private RestAPIExtension(String name, String displayName, String description, Artifact artifact) {
            super(name, displayName, description, artifact);
        }

    }

    public static class Page extends CustomPage {

        public static Page create(Properties properties, Artifact artifact) {
            String name = properties.getProperty("name");
            String displayName = properties.getProperty("displayName");
            String description = properties.getProperty("description");
            return new Page(name, displayName, description, artifact);
        }

        private Page(String name, String displayName, String description, Artifact artifact) {
            super(name, displayName, description, artifact);
        }

    }

    public static class Form extends CustomPage {

        public static Form create(Properties properties, Artifact artifact) {
            String name = properties.getProperty("name");
            String displayName = properties.getProperty("displayName");
            String description = properties.getProperty("description");
            return new Form(name, displayName, description, artifact);
        }

        private Form(String name, String displayName, String description, Artifact artifact) {
            super(name, displayName, description, artifact);
        }

    }

    public static class Theme extends CustomPage {

        public static Theme create(Properties properties, Artifact artifact) {
            String name = properties.getProperty("name");
            String displayName = properties.getProperty("displayName");
            String description = properties.getProperty("description");
            return new Theme(name, displayName, description, artifact);
        }

        private Theme(String name, String displayName, String description, Artifact artifact) {
            super(name, displayName, description, artifact);
        }

    }

    public static class Definition {

        private final String definitionId;
        private final String definitionVersion;
        private final Artifact artifact;

        public static Definition create(Document document, Artifact artifact) {
            String definitionId = readElement(document, "id");
            String definitionVersion = readElement(document, "version");
            return new Definition(definitionId, definitionVersion, artifact);
        }

        private Definition(String definitionId,
                String definitionVersion,
                Artifact artifact) {
            this.definitionId = definitionId;
            this.definitionVersion = definitionVersion;
            this.artifact = artifact;
        }

        public String getDefinitionId() {
            return definitionId;
        }

        public String getDefinitionVersion() {
            return definitionVersion;
        }

        public Artifact getArtifact() {
            return artifact;
        }

        @Override
        public String toString() {
            return definitionId + " (" + definitionVersion + ") in " + artifact;
        }
    }

    public static class Implementation {

        private final String className;
        private final String implementationId;
        private final String implementationVersion;
        private final String definitionId;
        private final String definitionVersion;
        private final Artifact artifact;
        private String superType;

        private Implementation(String className,
                String implementationId,
                String implementationVersion,
                String definitionId,
                String definitionVersion,
                Artifact artifact) {
            this.className = className;
            this.implementationId = implementationId;
            this.implementationVersion = implementationVersion;
            this.definitionId = definitionId;
            this.definitionVersion = definitionVersion;
            this.artifact = artifact;
        }

        public static Implementation create(Document document, Artifact artifact) {
            String className = readElement(document, "implementationClassname");
            String implementationId = readElement(document, "implementationId");
            String implementationVersion = readElement(document, "implementationVersion");
            String definitionId = readElement(document, "definitionId");
            String definitionVersion = readElement(document, "definitionVersion");
            return new Implementation(className, implementationId, implementationVersion, definitionId,
                    definitionVersion, artifact);
        }

        public String getClassName() {
            return className;
        }

        public String getImplementationId() {
            return implementationId;
        }

        public String getImplementationVersion() {
            return implementationVersion;
        }

        public String getDefinitionId() {
            return definitionId;
        }

        public String getDefinitionVersion() {
            return definitionVersion;
        }

        public Artifact getArtifact() {
            return artifact;
        }

        public String getSuperType() {
            return superType;
        }

        public void setSuperType(String type) {
            this.superType = type;
        }

        @Override
        public String toString() {
            return implementationId + " (" + implementationVersion + ") in " + artifact;
        }

    }

    static String readElement(Document document, String elementName) {
        return document.getElementsByTagName(elementName).item(0).getTextContent();
    }
}
