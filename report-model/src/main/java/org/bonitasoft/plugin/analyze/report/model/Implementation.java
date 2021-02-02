package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;

@Data
public class Implementation {

	private String className;

	private String implementationId;

	private String implementationVersion;

	private String definitionId;

	private String definitionVersion;

	private String path;

	private String filePath;

	private String superType;

	public static Implementation create(String className ,String definitionId, String definitionVersion, String implementationId, String implementationVersion, String entryPath, String filePath, String superType) {
		final Implementation implementation = new Implementation();
		implementation.setClassName(className);
		implementation.setDefinitionId(definitionId);
		implementation.setDefinitionVersion(definitionVersion);
		implementation.setImplementationId(implementationId);
		implementation.setImplementationVersion(implementationVersion);
		implementation.setFilePath(filePath);
		implementation.setSuperType(superType);
		return implementation;
	}

}
