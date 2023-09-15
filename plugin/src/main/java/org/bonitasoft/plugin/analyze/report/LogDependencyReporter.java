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

import static java.lang.String.format;

import org.apache.maven.plugin.logging.Log;
import org.bonitasoft.plugin.analyze.report.model.ApplicationDescriptor;
import org.bonitasoft.plugin.analyze.report.model.Artifact;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;

public class LogDependencyReporter implements DependencyReporter {

    private final Log log;

    public LogDependencyReporter(Log log) {
        this.log = log;
    }

    private static String asString(CustomPage customPage) {
        return customPage.getName() + asStringLocation(customPage.getArtifact());
    }

    private static String asStringLocation(Artifact artifact) {
        return " in " + artifact;
    }

    private static String asString(Definition definition) {
        return definition.getDefinitionId() + "-" + definition.getDefinitionVersion()
                + asStringLocation(definition.getArtifact());
    }

    private static String asString(Implementation implementation) {
        return implementation.getImplementationId() + "-" + implementation.getImplementationVersion()
                + " for " + implementation.getDefinitionId() + "-" + implementation.getDefinitionVersion()
                + asStringLocation(implementation.getArtifact());
    }

    private static String asString(ApplicationDescriptor descriptor) {
        return descriptor.getDisplayName() + "-" + descriptor.getVersion()
                + asStringLocation(descriptor.getArtifact());
    }

    @Override
    public void report(DependencyReport dependencyReport) {
        dependencyReport.getIssues().stream()
                .filter(issue -> Severity.valueOf(issue.getSeverity()) == Severity.ERROR)
                .forEach(issue -> log.error(issue.getMessage()));

        dependencyReport.getIssues().stream()
                .filter(issue -> Severity.valueOf(issue.getSeverity()) == Severity.WARNING)
                .forEach(issue -> log.warn(issue.getMessage()));

        dependencyReport.getIssues().stream()
                .filter(issue -> Severity.valueOf(issue.getSeverity()) == Severity.INFO)
                .forEach(issue -> log.info(issue.getMessage()));

        log.info(format("=== %s Connector definitions found ===", dependencyReport.getConnectorDefinitions().size()));
        dependencyReport.getConnectorDefinitions().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Connector implementations found ===",
                dependencyReport.getConnectorImplementations().size()));
        dependencyReport.getConnectorImplementations().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Actor filter definitions found ===", dependencyReport.getFilterDefinitions().size()));
        dependencyReport.getFilterDefinitions().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Actor filter implementations found ===",
                dependencyReport.getFilterImplementations().size()));
        dependencyReport.getFilterImplementations().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Pages found ===", dependencyReport.getPages().size()));
        dependencyReport.getPages().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Forms found", dependencyReport.getForms().size()));
        dependencyReport.getForms().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Rest API Extensions found ===", dependencyReport.getRestApiExtensions().size()));
        dependencyReport.getRestApiExtensions().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Themes found ===", dependencyReport.getThemes().size()));
        dependencyReport.getThemes().stream().map(LogDependencyReporter::asString).forEach(log::info);

        log.info(format("=== %s Application Descriptors found ===",
                dependencyReport.getApplicationDescriptors().size()));
        dependencyReport.getApplicationDescriptors().stream().map(LogDependencyReporter::asString).forEach(log::info);
    }
}
