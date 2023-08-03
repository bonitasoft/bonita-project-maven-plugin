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
package org.bonitasoft.plugin.build.page.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.rmi.server.ExportException;

import org.bonitasoft.plugin.build.page.BuildPageException;
import org.bonitasoft.web.designer.ArtifactBuilder;
import org.bonitasoft.web.designer.model.ModelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UidArtifactBuilderImplTest {

    @Mock
    private ArtifactBuilder artifactBuilder;

    @Test
    void buildPageWithNullId() throws Exception {
        var builder = new UidArtifactBuilderImpl(artifactBuilder);

        assertThrows(IllegalArgumentException.class, () -> builder.buildPage(null));
    }

    @Test
    void buildPageWithExportException() throws Exception {
        var builder = new UidArtifactBuilderImpl(artifactBuilder);
        when(artifactBuilder.buildPage("myPage")).thenThrow(ExportException.class);

        assertThrows(BuildPageException.class, () -> builder.buildPage("myPage"));
    }

    @Test
    void buildPageWithModelException() throws Exception {
        var builder = new UidArtifactBuilderImpl(artifactBuilder);
        when(artifactBuilder.buildPage("myPage")).thenThrow(ModelException.class);

        assertThrows(BuildPageException.class, () -> builder.buildPage("myPage"));
    }

    @Test
    void buildPageWithIOException() throws Exception {
        var builder = new UidArtifactBuilderImpl(artifactBuilder);
        when(artifactBuilder.buildPage("myPage")).thenThrow(IOException.class);

        assertThrows(BuildPageException.class, () -> builder.buildPage("myPage"));
    }

}
