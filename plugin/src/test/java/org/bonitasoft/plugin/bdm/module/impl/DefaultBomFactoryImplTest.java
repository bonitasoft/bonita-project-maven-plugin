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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.engine.bdm.BusinessObjectModelConverter;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.plugin.module.ModuleGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;

class DefaultBomFactoryImplTest {

    private static final String PROCUREMENT_EXAMPLE_PROJECT_ID = "procurement-example";
    private static final String PROCUREMENT_EXAMPLE_GROUP_ID = "org.company.example";
    private ModelReader modelReader = new DefaultModelReader();
    private ModelWriter modelWriter = new DefaultModelWriter();

    @TempDir
    private Path tmpDir;

    @Test
    void createDefaultBom() throws Exception {
        var returnedBom = createBom(tmpDir, PROCUREMENT_EXAMPLE_GROUP_ID);

        assertThat(returnedBom.getBusinessObjects().get(0).getQualifiedName())
                .isEqualTo(String.format("%s.model.%s", PROCUREMENT_EXAMPLE_GROUP_ID,
                        DefaultBomFactoryImpl.DEFAULT_BO_NAME));
    }

    @ParameterizedTest
    @ValueSource(strings = { "org.bonitasoft.example", "com.bonitasoft.example", "org.company-group-id" })
    void useDefaultPackagePrefixForNonCompliantPackageNameGroupIds(String packageName) throws Exception {
        var returnedBom = createBom(tmpDir, "org.bonitasoft.example");

        assertThat(returnedBom.getBusinessObjects().get(0).getQualifiedName())
                .isEqualTo(String.format("%s.model.%s", DefaultBomFactoryImpl.DEFAULT_PACKAGE_PREFIX,
                        DefaultBomFactoryImpl.DEFAULT_BO_NAME));
    }

    @Test
    void throwFileAlreadyExistsExceptionWhenBomAlreadyExist() throws Exception {
        var parentPomTemp = tmpDir.resolve(BdmModuleGeneratorImpl.POM_FILE_NAME);
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(DefaultBomFactoryImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());
        var generator = new BdmModuleGeneratorImpl(modelReader, modelWriter);
        var modulePath = generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject);

        var bomFactory = new DefaultBomFactoryImpl();
        bomFactory.createDefaultBom(mavenProject.getGroupId(), modulePath);

        assertThrows(FileAlreadyExistsException.class,
                () -> bomFactory.createDefaultBom(mavenProject.getGroupId(), modulePath),
                String.format("The %s for the module %s already exist for the project %s",
                        DefaultBomFactoryImpl.BOM_FILE_NAME,
                        modulePath.getFileName(), mavenProject.getGroupId()));
    }

    private BusinessObjectModel createBom(Path tmpDir, String groupId)
            throws IOException, ModuleGenerationException, JAXBException, SAXException {
        var parentPomTemp = tmpDir.resolve(BdmModuleGeneratorImpl.POM_FILE_NAME);
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(DefaultBomFactoryImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());
        mavenProject.setGroupId(groupId);
        var generator = new BdmModuleGeneratorImpl(modelReader, modelWriter);
        var modulePath = generator.create(PROCUREMENT_EXAMPLE_PROJECT_ID, mavenProject);

        var bomFactory = new DefaultBomFactoryImpl();
        var bomPath = bomFactory.createDefaultBom(mavenProject.getGroupId(), modulePath);
        assertThat(bomPath).exists().isNotEmptyFile();
        BusinessObjectModelConverter converter = new BusinessObjectModelConverter();
        BusinessObjectModel returnedBom = converter.unmarshall(Files.readAllBytes(bomPath));
        return returnedBom;
    }

}
