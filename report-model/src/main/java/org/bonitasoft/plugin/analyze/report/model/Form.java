package org.bonitasoft.plugin.analyze.report.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("FORM")
public class Form extends CustomPage {

	public static Form create(String name, String displayName, String description, Artifact artifact) {
		return CustomPage.create(name, displayName, description, artifact, Form.class);
	}

}
