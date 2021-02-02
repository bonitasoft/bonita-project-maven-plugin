package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@Data
@JsonTypeName("THEME")
public class Theme extends CustomPage {

	public static Theme create(String name, String displayName, String description, String filePath) {
		return CustomPage.create(name,displayName,description,filePath,Theme.class);
	}
}
