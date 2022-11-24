package org.bonitasoft.plugin.bdm.module.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
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
import org.bonitasoft.plugin.bdm.module.ModuleGenerationException;
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
        assertThat(bdmDaoClientModuleModel.getDependencies()).extracting("groupId","artifactId", "version")
            .contains(tuple("${project.groupId}","procurement-example-bdm-model", "${project.version}"));
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

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(ModuleGenerationException.class,
                () -> generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject));
        assertThat(exception.getMessage())
                .isEqualTo("Failed to add " + BdmModuleGeneratorImpl.BDM_PARENT_MODULE + " module to parent pom.");
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

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(ModuleGenerationException.class,
                () -> generator.createModule(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject,
                        parentPomTemp.getParent().resolve("bdm").resolve("dao-client"),
                        "/bdm.dao.module.xml", "-bdm-dao-client"));
        assertThat(exception.getMessage())
                .isEqualTo("Failed to write " + BdmModuleGeneratorImpl.DAO_CLIENT_MODULE_NAME + " module pom.");
    }

    @Test
    void throwMojoFailureExceptionWhenModuleAlreadyExist(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(BdmModuleGeneratorImpl.POM_FILE_NAME);
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());
        var generator = new BdmModuleGeneratorImpl(modelReader, modelWriter);

        generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject);

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(ModuleGenerationException.class,
                () -> generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject));
        assertThat(exception.getMessage()).isEqualTo("The module " + BdmModuleGeneratorImpl.BDM_PARENT_MODULE
                + " already exist for the project " + PROCUREMENT_EXAMPLE_PROJECT_ID);
    }

}
