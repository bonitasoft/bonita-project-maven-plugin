/**
 * Copyright (C) 2022 BonitaSoft S.A.
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
package org.bonitasoft.plugin.bdm.module;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Paths;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.build.incremental.BuildContext;

@ExtendWith(MockitoExtension.class)
class CreateBdmModuleMojoTest {

    @Mock
    private BuildContext context;
    @Mock
    private BdmModuleGenerator generator;
    @Mock
    private DefaultBomFactory bomFactory;
    
    @BeforeEach
    void setup() throws Exception {
        lenient().doReturn(Paths.get("")).when(generator).create(Mockito.anyString(), Mockito.any(MavenProject.class));
    }

    @Test
    void throwErrorWhenProjectDoesNotHavePomPackaging() throws Exception {
        var mojo = new CreateBdmModuleMojo(generator, bomFactory, context);
        mojo.bonitaProjectId = "my-project";
        var project = new MavenProject();
        project.setArtifactId("my-project-parent");
        project.setPackaging("jar");
        mojo.project = project;
        
        mojo.execute();
        
        verify(generator, never()).create("my-project", project);
    }
    
    @Test
    void throwErrorWhenProjectDoesNotHaveSameProjectId() throws Exception {
        var mojo = new CreateBdmModuleMojo(generator, bomFactory, context);
        mojo.bonitaProjectId = "my-project";
        var project = new MavenProject();
        project.setArtifactId("another-project-parent");
        project.setPackaging("pom");
        mojo.project = project;
        
        mojo.execute();
        
        verify(generator, never()).create("my-project", project);
    }
        
}
