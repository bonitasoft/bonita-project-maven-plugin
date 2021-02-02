package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@Type(value = RestAPIExtension.class, name = "APIEXTENSION"),
		@Type(value = Theme.class, name = "THEME"),
		@Type(value = Form.class, name = "FORM"),
		@Type(value = Page.class, name = "PAGE")
})
public abstract class CustomPage {

	public static final String DISPLAY_NAME_PROPERTY = "displayName";

	public static final String DESCRIPTION_PROPERTY = "description";

	public static final String NAME_PROPERTY = "name";

	private String name;

	private String displayName;

	private String description;

	private String filePath;

	protected static <T extends CustomPage> T create(String name, String displayName, String description, String filePath, Class<T> type) {
		try {
			T o = type.getDeclaredConstructor().newInstance();
			o.setName(name);
			o.setDisplayName(displayName);
			o.setDescription(description);
			o.setFilePath(filePath);
			return o;
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to create a new instance of class: " + type.getName(), e);
		}
	}

	public enum CustomPageType {
		FORM, PAGE, THEME, APIEXTENSION;
	}

}
