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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Definition;
import org.bonitasoft.plugin.analyze.BonitaArtifact.Implementation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConnectorResolverTest {

    private static final String GROUP_ID = "test";
    private static final String ARTIFACT_ID = "test";
    private static final String VERSION = "0.0.1";
    private static final String SCOPE = "compile";
    private static final String TYPE = "jar";
    private static final String CLASSIFIER = null;
    private Artifact artifact;
    
    @BeforeEach
    void createArtifact() throws Exception {
        artifact = new DefaultArtifact(GROUP_ID,ARTIFACT_ID,VERSION,SCOPE,TYPE,CLASSIFIER, new DefaultArtifactHandler());
    }

    @Test
    void testFindEmailConnectorImplementation() throws Exception {
        ConnectorResolver connectorTypeResolver = new ConnectorResolver();
        artifact.setFile(new File(ConnectorResolverTest.class.getResource("/bonita-connector-email-1.3.0.jar").getFile()));

        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact);

        assertEquals(1, implementations.size());
        Implementation emailImplementation = implementations.get(0);
        assertEquals(ConnectorResolver.ABSTRACT_CONNECTOR_TYPE, emailImplementation.getSuperType());
        assertEquals("email-impl", emailImplementation.getImplementationId());
        assertEquals("1.3.0", emailImplementation.getImplementationVersion());
        assertEquals("email", emailImplementation.getDefinitionId());
        assertEquals("1.2.0", emailImplementation.getDefinitionVersion());
    }
    
    @Test
    void testFindEmailConnectorDefinition() throws Exception {
        ConnectorResolver connectorTypeResolver = new ConnectorResolver();
        artifact.setFile(new File(ConnectorResolverTest.class.getResource("/bonita-connector-email-1.3.0.jar").getFile()));

        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact);

        assertEquals(1, definitions.size());
        Definition emailDefinition = definitions.get(0);
        assertEquals("email", emailDefinition.getDefinitionId());
        assertEquals("1.2.0", emailDefinition.getDefinitionVersion());
    }
    
    @Test
    void testFindRestConnectorDefinitions() throws Exception {
        ConnectorResolver connectorTypeResolver = new ConnectorResolver();
        artifact.setFile(new File(ConnectorResolverTest.class.getResource("/bonita-connector-rest-1.0.10.jar").getFile()));

        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact);

        assertEquals(4, definitions.size());
    }


    @Test
    void testFindAllRestConnectorImplementation() throws Exception {
        ConnectorResolver connectorTypeResolver = new ConnectorResolver();
        artifact.setFile(new File(ConnectorResolverTest.class.getResource("/bonita-connector-rest-1.0.10.jar").getFile()));

        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact);

        assertEquals(4, implementations.size());

        Optional<Implementation> postImplementationSearchResult = findImplemetationById("rest-post-impl",
                implementations);
        assertThat(postImplementationSearchResult)
                .isPresent()
                .get()
                .extracting("superType")
                .isEqualTo(ConnectorResolver.ABSTRACT_CONNECTOR_TYPE);

        Optional<Implementation> getImplementationSearchResult = findImplemetationById("rest-get-impl",
                implementations);
        assertThat(getImplementationSearchResult)
                .isPresent()
                .get()
                .extracting("superType")
                .isEqualTo(ConnectorResolver.ABSTRACT_CONNECTOR_TYPE);

        Optional<Implementation> putImplementationSearchResult = findImplemetationById("rest-put-impl",
                implementations);
        assertThat(putImplementationSearchResult)
                .isPresent()
                .get()
                .extracting("superType")
                .isEqualTo(ConnectorResolver.ABSTRACT_CONNECTOR_TYPE);

        Optional<Implementation> deleteImplementationSearchResult = findImplemetationById("rest-delete-impl",
                implementations);
        assertThat(deleteImplementationSearchResult)
                .isPresent()
                .get()
                .extracting("superType")
                .isEqualTo(ConnectorResolver.ABSTRACT_CONNECTOR_TYPE);
    }

    @Test
    void testFindSingleUserActorFilterImplementation() throws Exception {
        ConnectorResolver connectorTypeResolver = new ConnectorResolver();
        artifact.setFile(new File(ConnectorResolverTest.class.getResource("/bonita-actorfilter-single-user-1.0.0.jar").getFile()));

        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact);

        assertEquals(1, implementations.size());
        Implementation singleUserImplementation = implementations.get(0);
        assertEquals(ConnectorResolver.ABSTRACT_FILTER_TYPE, singleUserImplementation.getSuperType());
        assertEquals("bonita-actorfilter-single-user-impl", singleUserImplementation.getImplementationId());
        assertEquals("1.0.0", singleUserImplementation.getImplementationVersion());
        assertEquals("bonita-actorfilter-single-user", singleUserImplementation.getDefinitionId());
        assertEquals("1.0.0", singleUserImplementation.getDefinitionVersion());
    }
    
    @Test
    void testInvalidDescriptorsAreIgnored() throws Exception {
        ConnectorResolver connectorTypeResolver = new ConnectorResolver();
        artifact.setFile(new File(ConnectorResolverTest.class.getResource("/jar-with-invalid-descriptors.jar").getFile()));

        List<Implementation> implementations = connectorTypeResolver.findAllImplementations(artifact);
        List<Definition> definitions = connectorTypeResolver.findAllDefinitions(artifact);

        assertEquals(0, implementations.size());
        assertEquals(0, definitions.size());
    }
    
   

    private Optional<Implementation> findImplemetationById(String id, List<Implementation> implementations) {
        return implementations.stream()
                .filter(impl -> Objects.equals(id, impl.getImplementationId()))
                .findFirst();
    }

}
