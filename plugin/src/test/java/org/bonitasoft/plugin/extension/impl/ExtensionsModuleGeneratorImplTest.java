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
package org.bonitasoft.plugin.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;
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

class ExtensionsModuleGeneratorImplTest {

    private static final String PROCUREMENT_EXAMPLE_PROJECT_ID = "procurement-example";
    private ModelReader modelReader = new DefaultModelReader();
    private ModelWriter modelWriter = new DefaultModelWriter();

    @Test
    void generateExtensionModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(ExtensionsModuleGeneratorImpl.POM_FILE_NAME);
        var parentPom = new File(ExtensionsModuleGeneratorImpl.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        MavenProject mavenProject = new MavenProject(modelReader.read(parentPom, null));
        mavenProject.setFile(parentPomTemp.toFile());
        var generator = new ExtensionsModuleGeneratorImpl(modelReader, modelWriter);

        generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject);

        Path extensionModule = tmpDir.resolve("extensions");
        assertThat(extensionModule).exists().isDirectoryContaining(
                path -> path.getFileName().toString().equals(ExtensionsModuleGeneratorImpl.POM_FILE_NAME));
        var extensionModuleModel = modelReader.read(extensionModule.resolve("pom.xml").toFile(), null);
        assertThat(extensionModuleModel.getParent().getGroupId()).isEqualTo("com.bonitasoft.example");
        assertThat(extensionModuleModel.getParent().getArtifactId()).isEqualTo("procurement-example-parent");
        assertThat(extensionModuleModel.getParent().getVersion()).isEqualTo("2.1");
        assertThat(extensionModuleModel.getArtifactId()).isEqualTo("procurement-example-extensions");
    }

    @Test
    void throwModuleGenerationExceptionWhenFailToAddExtensionParentModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(ExtensionsModuleGeneratorImpl.POM_FILE_NAME);
        var mockedModelWriter = mock(ModelWriter.class);
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(ExtensionsModuleGeneratorImpl.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());

        doThrow(IOException.class).when(mockedModelWriter)
                .write(Mockito.eq(mavenProject.getFile()), ArgumentMatchers.isNull(), Mockito.any(Model.class));

        var generator = new ExtensionsModuleGeneratorImpl(modelReader, mockedModelWriter);

        assertThrows(ModuleGenerationException.class,
                () -> generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject),
                "Failed to add " + ExtensionsModuleGeneratorImpl.EXTENSIONS_PARENT_MODULE + " module to parent pom.");
    }

    @Test
    void throwModuleGenerationExceptionWhenFailToAddExntesionsModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve(ExtensionsModuleGeneratorImpl.POM_FILE_NAME);
        var mockedModelWriter = mock(ModelWriter.class);

        var parentPom = new File(ExtensionsModuleGeneratorImpl.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        var mavenProject = new DefaultModelReader().read(parentPom, null);

        doThrow(IOException.class).when(mockedModelWriter)
                .write(Mockito.any(OutputStream.class), ArgumentMatchers.isNull(), Mockito.any(Model.class));

        var generator = new ExtensionsModuleGeneratorImpl(modelReader, mockedModelWriter);

        assertThrows(ModuleGenerationException.class,
                () -> generator.createModule(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject,
                        parentPomTemp.getParent().resolve("extensions"),
                        "/extensions.parent.module.xml", "-extensions"),
                "Failed to write " + ExtensionsModuleGeneratorImpl.EXTENSIONS_PARENT_MODULE + " module pom.");
    }

}
