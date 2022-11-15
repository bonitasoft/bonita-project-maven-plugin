/**
 * 
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
        lenient().when(buildContext.hasDelta(Mockito.any(File.class))).thenReturn(true);
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

        assertThat(packageFolder).isDirectoryNotContaining(path -> path.getFileName().toString().equals("Quotation.java"))
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
