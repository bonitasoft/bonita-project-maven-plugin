/** 
 * Copyright (C) 2020 BonitaSoft S.A.
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
package org.bonitasoft.plugin.analyze.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.ConnectorResolver;
import org.bonitasoft.plugin.analyze.content.JarArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.model.ActorFilterImplementation;
import org.bonitasoft.plugin.analyze.report.model.ConnectorImplementation;
import org.bonitasoft.plugin.analyze.report.model.Definition;
import org.bonitasoft.plugin.analyze.report.model.Implementation;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConnectorResolverImplTest {

    private static final String GROUP_ID = "test";

    private static final String ARTIFACT_ID = "test";

    private static final String VERSION = "0.0.1";

    private static final String SCOPE = "compile";

    private static final String TYPE = "jar";

    private static final String CLASSIFIER = null;

    private Artifact artifact;

    @BeforeEach
    void createArtifact() {
        artifact = new DefaultArtifact(GROUP_ID, ARTIFACT_ID, VERSION, SCOPE, TYPE, CLASSIFIER,
                new DefaultArtifactHandler());
    }

    @Test
    void testFindEmailConnectorImplementation() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(ConnectorResolverImplTest.class.getResource("/bonita-connector-email-1.3.0.jar").getFile()));

        // when
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                Issue.collector());

        // then
        assertEquals(1, implementations.size());
        Implementation emailImplementation = implementations.get(0);
        assertTrue(emailImplementation instanceof ConnectorImplementation);
        assertEquals("email-impl", emailImplementation.getImplementationId());
        assertEquals("1.3.0", emailImplementation.getImplementationVersion());
        assertEquals("email", emailImplementation.getDefinitionId());
        assertEquals("1.2.0", emailImplementation.getDefinitionVersion());
    }

    @Test
    void testFindEmailConnectorDefinition() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(ConnectorResolverImplTest.class.getResource("/bonita-connector-email-1.3.0.jar").getFile()));

        // when
        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact, reader, Issue.collector());

        // then
        assertEquals(1, definitions.size());
        Definition emailDefinition = definitions.get(0);
        assertEquals("email", emailDefinition.getDefinitionId());
        assertEquals("1.2.0", emailDefinition.getDefinitionVersion());
    }

    @Test
    void testFindRestConnectorDefinitions() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(ConnectorResolverImplTest.class.getResource("/bonita-connector-rest-1.0.10.jar").getFile()));

        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact, reader, Issue.collector());

        assertEquals(4, definitions.size());
    }

    @Test
    void testFindAllRestConnectorImplementation() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(ConnectorResolverImplTest.class.getResource("/bonita-connector-rest-1.0.10.jar").getFile()));

        // when
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                Issue.collector());

        // then
        assertEquals(4, implementations.size());

        Optional<Implementation> postImplementationSearchResult = findImplemetationById("rest-post-impl",
                implementations);
        assertThat(postImplementationSearchResult)
                .isPresent()
                .get()
                .isInstanceOf(ConnectorImplementation.class);

        Optional<Implementation> getImplementationSearchResult = findImplemetationById("rest-get-impl",
                implementations);
        assertThat(getImplementationSearchResult)
                .isPresent()
                .get()
                .isInstanceOf(ConnectorImplementation.class);

        Optional<Implementation> putImplementationSearchResult = findImplemetationById("rest-put-impl",
                implementations);
        assertThat(putImplementationSearchResult)
                .isPresent()
                .get()
                .isInstanceOf(ConnectorImplementation.class);

        Optional<Implementation> deleteImplementationSearchResult = findImplemetationById("rest-delete-impl",
                implementations);
        assertThat(deleteImplementationSearchResult)
                .isPresent()
                .get()
                .isInstanceOf(ConnectorImplementation.class);
    }

    @Test
    void testFindAllDatabaseConnectorImplementation() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(
                        ConnectorResolverImplTest.class.getResource("/bonita-connector-database-2.0.3.jar").getFile()));

        // when
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                Issue.collector());

        // then
        assertEquals(17, implementations.size());
    }

    @Test
    void testFindSingleUserActorFilterImplementation() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(new File(
                ConnectorResolverImplTest.class.getResource("/bonita-actorfilter-single-user-1.0.0.jar").getFile()));

        // when
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                Issue.collector());

        // then
        assertEquals(1, implementations.size());
        Implementation singleUserImplementation = implementations.get(0);
        assertTrue(singleUserImplementation instanceof ActorFilterImplementation);
        assertEquals("bonita-actorfilter-single-user-impl", singleUserImplementation.getImplementationId());
        assertEquals("1.0.0", singleUserImplementation.getImplementationVersion());
        assertEquals("bonita-actorfilter-single-user", singleUserImplementation.getDefinitionId());
        assertEquals("1.0.0", singleUserImplementation.getDefinitionVersion());
    }

    @Test
    void testInvalidDescriptorsFillIssues() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(ConnectorResolverImplTest.class.getResource("/jar-with-invalid-descriptors.jar").getFile()));

        // when
        var issueCollector = Issue.collector();
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                issueCollector);
        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact, reader, issueCollector);

        // then
        assertEquals(0, implementations.size());
        assertEquals(0, definitions.size());
        assertThat(issueCollector.getIssues())
                .hasSize(6)
                .allSatisfy(issue -> assertThat(issue.getType()).isEqualTo(Type.INVALID_DESCRIPTOR_FILE.name()))
                .allSatisfy(issue -> assertThat(issue.getSeverity()).isEqualTo(Severity.ERROR.name()))
                .allSatisfy(issue -> assertThat(issue.getContext()).isEqualTo(List.of(artifact.getId())))
                .anySatisfy(issue -> assertThat(issue.getMessage()).isEqualTo(
                        "email-invalid-classname.impl declares an unknown 'implementationClassname': org.bonitasoft.connectors.email.EmailConnector"))
                .anySatisfy(issue -> assertThat(issue.getMessage()).isEqualTo(
                        "email.impl is not compliant with 'http://www.bonitasoft.org/ns/connector/implementation/6.0' XML schema definition"))
                .anySatisfy(issue -> assertThat(issue.getMessage()).isEqualTo(
                        "email.def is not compliant with 'http://www.bonitasoft.org/ns/connector/definition/6.1' XML schema definition"))
                .anySatisfy(issue -> assertThat(issue.getMessage()).isEqualTo(
                        "somethingElse.def is not compliant with 'http://www.bonitasoft.org/ns/connector/definition/6.1' XML schema definition"))
                .anySatisfy(issue -> assertThat(issue.getMessage())
                        .startsWith("notXmlContent.def is not a valid XML file:"))
                .anySatisfy(issue -> assertThat(issue.getMessage())
                        .startsWith("email-with-typo.def is not a valid XML file:"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/connector-with-invalid-implemetation-class.jar",
            "/connector-with-missing-implementation-class.jar" })
    void testJarWithInvalidImplemetationClasses(String testJarFile) throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(new File(
                ConnectorResolverImplTest.class.getResource(testJarFile).getFile()));

        // when
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                Issue.collector());
        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact, reader, Issue.collector());

        // then
        assertEquals(0, implementations.size());
        assertEquals(1, definitions.size());
    }

    @Test
    void testCustomConnectorFailingWithProcyon() throws Exception {
        // given
        ConnectorResolver connectorTypeResolver = new ConnectorResolverImpl();
        JarArtifactContentReader reader = new JarArtifactContentReader();
        artifact.setFile(
                new File(ConnectorResolverImplTest.class
                        .getResource("/bonita-connector-email-templating-2.5-SNAPSHOT.jar").getFile()));

        // when
        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact, reader,
                Issue.collector());
        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact, reader, Issue.collector());

        // then
        assertEquals(1, implementations.size());
        assertEquals(1, definitions.size());
    }

    private Optional<Implementation> findImplemetationById(String id, List<Implementation> implementations) {
        return implementations.stream()
                .filter(impl -> Objects.equals(id, impl.getImplementationId()))
                .findFirst();
    }

}
