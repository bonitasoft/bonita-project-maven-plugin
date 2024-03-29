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
package org.bonitasoft.plugin.bdm.module.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.module.ModuleGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class BdmModuleGeneratorImplTest {

    private static final String PROCUREMENT_EXAMPLE_PROJECT_ID = "procurement-example";
    private ModelReader modelReader = new DefaultModelReader();
    private ModelWriter modelWriter = new DefaultModelWriter();

    @Test
    void generateBdmModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(BdmModuleGeneratorImpl.POM_FILE_NAME);
        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        MavenProject mavenProject = new MavenProject(modelReader.read(parentPom, null));
        mavenProject.setFile(parentPomTemp.toFile());
        var generator = new BdmModuleGeneratorImpl(modelReader, modelWriter);

        generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject);

        Path bdmModule = tmpDir.resolve("bdm");
        assertThat(bdmModule).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(BdmModuleGeneratorImpl.POM_FILE_NAME));
        var bdmModuleModel = modelReader.read(bdmModule.resolve("pom.xml").toFile(), null);
        assertThat(bdmModuleModel.getParent().getGroupId()).isEqualTo("com.bonitasoft.example");
        assertThat(bdmModuleModel.getParent().getArtifactId()).isEqualTo("procurement-example-parent");
        assertThat(bdmModuleModel.getParent().getVersion()).isEqualTo("2.1");
        assertThat(bdmModuleModel.getArtifactId()).isEqualTo("procurement-example-bdm-parent");

        Path modelModule = tmpDir.resolve("bdm").resolve("model");
        assertThat(modelModule).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(BdmModuleGeneratorImpl.POM_FILE_NAME));
        var bdmModelModuleModel = modelReader.read(modelModule.resolve("pom.xml").toFile(), null);
        assertThat(bdmModelModuleModel.getParent().getGroupId()).isEqualTo("com.bonitasoft.example");
        assertThat(bdmModelModuleModel.getParent().getArtifactId()).isEqualTo("procurement-example-bdm-parent");
        assertThat(bdmModelModuleModel.getParent().getVersion()).isEqualTo("2.1");
        assertThat(bdmModelModuleModel.getArtifactId()).isEqualTo("procurement-example-bdm-model");

        var daoClientModule = tmpDir.resolve("bdm").resolve("dao-client");
        assertThat(daoClientModule).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(BdmModuleGeneratorImpl.POM_FILE_NAME));
        var bdmDaoClientModuleModel = modelReader.read(daoClientModule.resolve("pom.xml").toFile(), null);
        assertThat(bdmDaoClientModuleModel.getParent().getGroupId()).isEqualTo("com.bonitasoft.example");
        assertThat(bdmDaoClientModuleModel.getParent().getArtifactId()).isEqualTo("procurement-example-bdm-parent");
        assertThat(bdmDaoClientModuleModel.getParent().getVersion()).isEqualTo("2.1");
        assertThat(bdmDaoClientModuleModel.getArtifactId()).isEqualTo("procurement-example-bdm-dao-client");
        assertThat(bdmDaoClientModuleModel.getDependencies()).extracting("groupId", "artifactId", "version")
                .contains(tuple("${project.groupId}", "procurement-example-bdm-model", "${project.version}"));
    }

    @Test
    void throwModuleGenerationExceptionWhenFailToAddBdmParentModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(BdmModuleGeneratorImpl.POM_FILE_NAME);
        var mockedModelWriter = mock(ModelWriter.class);
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());

        doThrow(IOException.class).when(mockedModelWriter)
                .write(Mockito.eq(mavenProject.getFile()), ArgumentMatchers.isNull(), Mockito.any(Model.class));

        var generator = new BdmModuleGeneratorImpl(modelReader, mockedModelWriter);

        assertThrows(ModuleGenerationException.class,
                () -> generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject),
                "Failed to add " + BdmModuleGeneratorImpl.BDM_PARENT_MODULE + " module to parent pom.");
    }

    @Test
    void throwModuleGenerationExceptionWhenFailToAddBdmModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(BdmModuleGeneratorImpl.POM_FILE_NAME);
        var mockedModelWriter = mock(ModelWriter.class);

        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        var mavenProject = new DefaultModelReader().read(parentPom, null);

        doThrow(IOException.class).when(mockedModelWriter)
                .write(Mockito.any(OutputStream.class), ArgumentMatchers.isNull(), Mockito.any(Model.class));

        var generator = new BdmModuleGeneratorImpl(modelReader, mockedModelWriter);

        assertThrows(ModuleGenerationException.class,
                () -> generator.createModule(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject,
                        parentPomTemp.getParent().resolve("bdm").resolve("dao-client"),
                        "/bdm.dao.module.xml", "-bdm-dao-client"),
                "Failed to write " + BdmModuleGeneratorImpl.DAO_CLIENT_MODULE_NAME + " module pom.");
    }

}
