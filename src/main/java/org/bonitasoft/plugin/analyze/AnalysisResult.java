/**
 * Copyright (C) 2020 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.plugin.analyze.BonitaArtifact.Definition;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Form;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Implementation;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Page;
import org.bonitasoft.plugin.analyze.BonitaArtifact.RestAPIExtension;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Theme;

public class AnalysisResult {

	private final List<Implementation> connectorImplmentations = new ArrayList<>();

	private final List<Implementation> filterImplmentations = new ArrayList<>();

	private final List<Definition> connectorDefinitions = new ArrayList<>();

	private final List<Definition> filterDefinitions = new ArrayList<>();

	private final List<RestAPIExtension> restApiExtensions = new ArrayList<>();

	private final List<Page> pages = new ArrayList<>();

	private final List<Form> forms = new ArrayList<>();

	private final List<Theme> themes = new ArrayList<>();

	public void addConnectorImplementation(Implementation connectorImplementation) {
		connectorImplmentations.add(connectorImplementation);
	}

	public void addFilterImplementation(Implementation filterImplementation) {
		filterImplmentations.add(filterImplementation);
	}

	public void addConnectorDefinition(Definition connectorDefinition) {
		connectorDefinitions.add(connectorDefinition);
	}

	public void addFilterDefinition(Definition filterDefinition) {
		filterDefinitions.add(filterDefinition);
	}

	public void addRestAPIExtension(RestAPIExtension restApiExtension) {
		restApiExtensions.add(restApiExtension);
	}

	public void addPage(Page page) {
		pages.add(page);
	}

	public void addForm(Form form) {
		forms.add(form);
	}

	public void addTheme(Theme theme) {
		themes.add(theme);
	}

	public List<Implementation> getConnectorImplmentations() {
		return connectorImplmentations;
	}

	public List<Implementation> getFilterImplmentations() {
		return filterImplmentations;
	}

	public List<Definition> getConnectorDefinitions() {
		return connectorDefinitions;
	}

	public List<Definition> getFilterDefinitions() {
		return filterDefinitions;
	}

	public List<RestAPIExtension> getRestApiExtensions() {
		return restApiExtensions;
	}

	public List<Page> getPages() {
		return pages;
	}

	public List<Form> getForms() {
		return forms;
	}

	public List<Theme> getThemes() {
		return themes;
	}

}
