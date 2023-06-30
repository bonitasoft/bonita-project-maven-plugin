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
package org.bonitasoft.plugin.bdm.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.bdm.codegen.impl.BusinessDataModelGeneratorImpl;
import org.bonitasoft.plugin.bdm.codegen.impl.BusinessDataModelParserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.build.incremental.BuildContext;

@ExtendWith(MockitoExtension.class)
class GenerateBdmDaoClientSourceMojoTest {

    @Mock
    BuildContext buildContext;

    @BeforeEach
    void configureBuildContext() throws Exception {
        lenient().when(buildContext.hasDelta(Mockito.any(String.class))).thenReturn(true);
    }

    @Test
    void generateBdmDaoClientSources(@TempDir Path outputFolder) throws Exception {
        Files.delete(outputFolder);

        var mojo = new GenerateBdmDaoClientSourceMojo(new BusinessDataModelParserImpl(),
                new BusinessDataModelGeneratorImpl(), buildContext);
        mojo.bdmModelFile = new File(GenerateBdmDaoClientSourceMojoTest.class.getResource("/bom.xml").getFile());
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();

        mojo.execute();

        verify(buildContext).refresh(outputFolder.toFile());

        var packageFolder = outputFolder
                .resolve("com")
                .resolve("company")
                .resolve("model");

        assertThat(packageFolder)
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("Quotation.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("QuotationDAO.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("Supplier.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("SupplierDAO.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("Request.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("RequestDAO.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("QuotationDAOImpl.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("SupplierDAOImpl.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("RequestDAOImpl.java"));
    }
}
