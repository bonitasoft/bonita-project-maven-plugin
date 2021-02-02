package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@Data
@JsonTypeName("PAGE")
public class Page extends CustomPage {

	public static Page create(String name, String displayName, String description, String filePath) {
		return CustomPage.create(name,displayName,description,filePath,Page.class);
	}
}
