package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@Data
@JsonTypeName("APIEXTENSION")
public class RestAPIExtension extends CustomPage {

	public static RestAPIExtension create(String name, String displayName, String description, String filePath) {
		return CustomPage.create(name,displayName,description,filePath,RestAPIExtension.class);
	}

}
