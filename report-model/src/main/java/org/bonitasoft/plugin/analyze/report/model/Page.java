package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("PAGE")
public class Page extends CustomPage {

	public static Page create(String name, String displayName, String description, Artifact artifact) {
		return CustomPage.create(name,displayName,description,artifact,Page.class);
	}
}
