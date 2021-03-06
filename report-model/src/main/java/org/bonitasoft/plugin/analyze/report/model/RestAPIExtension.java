package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("APIEXTENSION")
public class RestAPIExtension extends CustomPage {

	public static RestAPIExtension create(String name, String displayName, String description, Artifact artifact) {
		return CustomPage.create(name,displayName,description,artifact,RestAPIExtension.class);
	}

}
