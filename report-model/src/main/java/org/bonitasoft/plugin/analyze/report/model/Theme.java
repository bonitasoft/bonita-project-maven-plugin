package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("THEME")
public class Theme extends CustomPage {

	public static Theme create(String name, String displayName, String description, Artifact artifact) {
		return CustomPage.create(name,displayName,description,artifact,Theme.class);
	}
}
