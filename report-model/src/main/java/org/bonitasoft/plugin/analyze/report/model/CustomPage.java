package org.bonitasoft.plugin.analyze.report.model;

public abstract class CustomPage {

	public static final String DISPLAY_NAME_PROPERTY = "displayName";

	public static final String DESCRIPTION_PROPERTY = "description";

	public static final String NAME_PROPERTY = "name";

	private final String name;

	private final String displayName;

	private final String description;

	private final String filePath;

	public CustomPage(String name, String displayName, String description, String filePath) {
		this.name = name;
		this.displayName = displayName;
		this.description = description;
		this.filePath = filePath;
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

	public String getFilePath() {
		return filePath;
	}

	@Override
	public String toString() {
		return displayName + " in " + filePath;
	}

}
