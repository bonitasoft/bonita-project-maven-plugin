package org.bonitasoft.plugin.analyze.report.model;

import lombok.Data;

@Data
public class Definition {

	private String definitionId;

	private String definitionVersion;

	private String filePath;

	private String entryPath;

	public static Definition create(String definitionId, String definitionVersion, String entryPath, String filePath) {
		final Definition definition = new Definition();
		definition.setDefinitionId(definitionId);
		definition.setDefinitionVersion(definitionVersion);
		definition.setEntryPath(entryPath);
		definition.setFilePath(filePath);
		return definition;
	}
}
