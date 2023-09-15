/** 
 * Copyright (C) 2023 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.analyze.report;

import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ApplicationDescriptor;
import org.bonitasoft.plugin.analyze.report.model.Artifact;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.DescriptorIdentifier;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.junit.jupiter.api.Test;

abstract class AbstractDependencyReportReporterTest {

    protected abstract DependencyReporter getReporter();

    @Test
    void should_write_report() throws Exception {
        // Given
        final DependencyReport dependencyReport = new DependencyReport();
        Artifact connectorArtifact = Artifact.create("org.bonita", "a-connector", "1.0.0", null,
                "/tmp/a-connector-1.0.0.jar");
        dependencyReport.addConnectorImplementation(ConnectorImplementation.create(
                "aClassName",
                new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
                new DescriptorIdentifier("anImplementationId", "anImplementationVersion"),
                connectorArtifact,
                "connector.impl"));
        dependencyReport.addConnectorDefinition(Definition.create(
                new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
                connectorArtifact,
                "connector.def"));

        Artifact filterArtifact = Artifact.create("org.bonita", "a-filter", "1.0.0", null, "/tmp/a-filter-1.0.0.jar");
        dependencyReport.addFilterImplementation(ActorFilterImplementation.create(
                "aClassName",
                new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
                new DescriptorIdentifier("anImplementationId", "anImplementationVersion"),
                filterArtifact,
                "filter.impl"));
        dependencyReport.addFilterDefinition(Definition.create(
                new DescriptorIdentifier("aDefinitionId", "aDefinitionVersion"),
                filterArtifact,
                "filter.def"));

        dependencyReport.addRestAPIExtension(RestAPIExtension.create("rest_name",
                "rest_displayName",
                "rest_description",
                Artifact.create("org.bonita", "a-rest-api", "1.0.0", null, "/tmp/a-rest-1.0.0.jar")));
        dependencyReport.addPage(Page.create("page_name",
                "page_displayName",
                "page_description",
                Artifact.create("org.bonita", "a-page", "1.0.0", null, "/tmp/a-page-1.0.0.jar")));
        dependencyReport.addForm(Form.create("form_name",
                "form_displayName",
                "form_description",
                Artifact.create("org.bonita", "a-form", "1.0.0", null, "/tmp/a-form-1.0.0.zip")));
        dependencyReport.addTheme(Theme.create("theme_name",
                "theme_displayName",
                "theme_description",
                Artifact.create("org.bonita", "a-theme", "1.0.0", null, "/tmp/a-theme-1.0.0.zip")));
        dependencyReport.addApplicationDescriptor(ApplicationDescriptor.create("My App",
                "1.0",
                "app_description",
                "User",
                "appToken",
                Artifact.create("org.bonita", "a-app", "1.0.0", "application", "/tmp/a-application-1.0.0.zip")));

        // When
        getReporter().report(dependencyReport);

        // Then
        assertReportIsValid();
    }

    protected abstract void assertReportIsValid() throws Exception;

}
