package org.bonitasoft.plugin.analyze.report.model;

public class Implementation {

	private final String className;

	private final String implementationId;

	private final String implementationVersion;

	private final String definitionId;

	private final String definitionVersion;

	private final String path;

	private final String filePath;

	private String superType;


	 public Implementation(String className,
			String implementationId,
			String implementationVersion,
			String definitionId,
			String definitionVersion,
			String path,
			String filePath) {
		this.className = className;
		this.implementationId = implementationId;
		this.implementationVersion = implementationVersion;
		this.definitionId = definitionId;
		this.definitionVersion = definitionVersion;
		this.path = path;
		this.filePath = filePath;
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

	public String getPath() {
		return path;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getSuperType() {
		return superType;
	}

	public void setSuperType(String type) {
		this.superType = type;
	}

	@Override
	public String toString() {
		return implementationId + " (" + implementationVersion + ") in " + filePath;
	}

}
