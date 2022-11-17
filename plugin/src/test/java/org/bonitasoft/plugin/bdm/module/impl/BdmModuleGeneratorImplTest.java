package org.bonitasoft.plugin.bdm.module.impl;

import static org.assertj.core.api.Assertions.assertThat;
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
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());
        var generator = new BdmModuleGeneratorImpl(modelReader, modelWriter);

        generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject);

        assertThat(tmpDir.resolve("bdm")).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(BdmModuleGeneratorImpl.POM_FILE_NAME));
        assertThat(tmpDir.resolve("bdm").resolve("model")).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(BdmModuleGeneratorImpl.POM_FILE_NAME));
        assertThat(tmpDir.resolve("bdm").resolve("dao-client")).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(BdmModuleGeneratorImpl.POM_FILE_NAME));
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
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());

        doThrow(IOException.class).when(mockedModelWriter)
                .write(Mockito.any(OutputStream.class), ArgumentMatchers.isNull(), Mockito.any(Model.class));

        var generator = new BdmModuleGeneratorImpl(modelReader, mockedModelWriter);

        Exception exception = org.junit.jupiter.api.Assertions.assertThrows(ModuleGenerationException.class,
                () -> generator.createModule(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject,
                        mavenProject.getBasedir().toPath().resolve("bdm").resolve("dao-client"),
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
