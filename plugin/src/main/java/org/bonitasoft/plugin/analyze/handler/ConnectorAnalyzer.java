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
package org.bonitasoft.plugin.analyze.handler;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.ConnectorResolver;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnectorAnalyzer extends AbstractArtifactAnalyzerHandler {

    protected static final String CONNECTOR_DESCRIPTOR_EXTENSION = "impl";

    protected static final Logger LOGGER = LoggerFactory.getLogger(ConnectorAnalyzer.class);
    private ConnectorResolver connectorResolver;

    ConnectorAnalyzer(LocalRepositoryManager localRepositoryManager, ConnectorResolver connectorResolver,
            ArtifactContentReader contentReader) {
        super(localRepositoryManager, contentReader);
        this.connectorResolver = connectorResolver;
    }

    @Override
    public boolean appliesTo(Artifact artifact) {
        return super.appliesTo(artifact) && hasConnectorDescriptor(artifact);
    }

    @Override
    public DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException {
        var issueCollector = Issue.collector();
        List<Implementation> allImplementations = connectorResolver.findAllImplementations(artifact, getContentReader(),
                issueCollector);
        List<Definition> allDefinitions = connectorResolver.findAllDefinitions(artifact, getContentReader(),
                issueCollector);
        List<ConnectorImplementation> connectorImplementations = allImplementations.stream()
                .filter(ConnectorImplementation.class::isInstance).map(ConnectorImplementation.class::cast)
                .collect(toList());
        List<ActorFilterImplementation> filterImplementations = allImplementations.stream()
                .filter(ActorFilterImplementation.class::isInstance).map(ActorFilterImplementation.class::cast)
                .collect(toList());
        allDefinitions.stream().filter(def -> hasMatchingImplementation(def, connectorImplementations))
                .forEach(report::addConnectorDefinition);
        allDefinitions.stream().filter(def -> hasMatchingImplementation(def, filterImplementations))
                .forEach(report::addFilterDefinition);
        allDefinitions.stream()
                .filter(def -> !hasMatchingImplementation(def,
                        Stream.of(connectorImplementations, filterImplementations).flatMap(Collection::stream)
                                .collect(Collectors.toList())))
                .map(def -> Issue.create(Type.UNKNOWN_DEFINITION_TYPE, String.format(
                        "%s declares a definition '%s (%s)' but no matching implementation has been found. This definition will be ignored.",
                        def.getJarEntry(), def.getDefinitionId(), def.getDefinitionVersion()), Severity.WARNING,
                        artifact.getId()))
                .forEach(report::addIssue);
        connectorImplementations.forEach(report::addConnectorImplementation);
        filterImplementations.forEach(report::addFilterImplementation);
        issueCollector.getIssues().forEach(report::addIssue);
        return report;
    }

    boolean hasMatchingImplementation(Definition def,
            List<? extends Implementation> connectorImplementations) {
        return connectorImplementations.stream()
                .anyMatch(implementation -> Objects.equals(def.getDefinitionId(), implementation.getDefinitionId())
                        && Objects.equals(def.getDefinitionVersion(), implementation.getDefinitionVersion()));
    }

    boolean hasConnectorDescriptor(Artifact artifact) {
        return getContentReader()
                .hasEntryWithPath(artifact,
                        path -> path.getFileName().toString().endsWith("." + CONNECTOR_DESCRIPTOR_EXTENSION));
    }
}
