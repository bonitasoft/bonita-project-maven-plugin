package org.bonitasoft.plugin.analyze.report.model;

public class Definition {

	private final String definitionId;
	private final String definitionVersion;
	private final String filePath;
	private String entryPath;

	public Definition(String definitionId,
			String definitionVersion,
			String entryPath,
			String filePath) {
		this.definitionId = definitionId;
		this.definitionVersion = definitionVersion;
		this.entryPath = entryPath;
		this.filePath = filePath;
	}

	public String getDefinitionId() {
		return definitionId;
	}

	public String getDefinitionVersion() {
		return definitionVersion;
	}

	public String getEntryPath() {
		return entryPath;
	}

	public String getFilePath() {
		return filePath;
	}

	@Override
	public String toString() {
		return definitionId + " (" + definitionVersion + ") in " + filePath;
	}

}
