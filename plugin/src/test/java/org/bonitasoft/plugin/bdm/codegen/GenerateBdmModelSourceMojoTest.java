/**
 * 
 */
package org.bonitasoft.plugin.bdm.codegen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.plugin.bdm.codegen.impl.BusinessDataModelGeneratorImpl;
import org.bonitasoft.plugin.bdm.codegen.impl.BusinessDataModelParserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.build.incremental.BuildContext;

@ExtendWith(MockitoExtension.class)
class GenerateBdmModelSourceMojoTest {

    @Mock(strictness = Strictness.LENIENT)
    BuildContext buildContext;

    @BeforeEach
    void configureBuildContext() throws Exception {
        when(buildContext.hasDelta(Mockito.any(String.class))).thenReturn(true);
    }

    @Test
    void generateBdmModelSourcesInEmptyOutputFolder(@TempDir Path outputFolder) throws Exception {
        Files.delete(outputFolder);
        
        var mojo = new GenerateBdmModelSourceMojo(new BusinessDataModelParserImpl(),
                new BusinessDataModelGeneratorImpl(), buildContext);
        mojo.bdmModelFile = new File(GenerateBdmModelSourceMojoTest.class.getResource("/bom.xml").getFile());
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();

        mojo.execute();

        verify(buildContext).refresh(outputFolder.toFile());
        
        var packageFolder = outputFolder
                .resolve("com")
                .resolve("company")
                .resolve("model");

        assertThat(packageFolder).isDirectoryContaining(path -> path.getFileName().toString().equals("Quotation.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("QuotationDAO.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("Supplier.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("SupplierDAO.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("Request.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("RequestDAO.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("QuotationDAOImpl.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("SupplierDAOImpl.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("RequestDAOImpl.java"));
    }
    
    @Test
    void generateBdmModelSourcesInExistingOutputFolder(@TempDir Path outputFolder) throws Exception {
        Files.delete(outputFolder);
        
        var mojo = new GenerateBdmModelSourceMojo(new BusinessDataModelParserImpl(),
                new BusinessDataModelGeneratorImpl(), buildContext);
        mojo.bdmModelFile = new File(GenerateBdmModelSourceMojoTest.class.getResource("/bom.xml").getFile());
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();

        mojo.execute();

        var packageFolder = outputFolder
                .resolve("com")
                .resolve("company")
                .resolve("model");

        assertThat(packageFolder).isDirectoryContaining(path -> path.getFileName().toString().equals("Quotation.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("QuotationDAO.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("Supplier.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("SupplierDAO.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("Request.java"))
                .isDirectoryContaining(path -> path.getFileName().toString().equals("RequestDAO.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("QuotationDAOImpl.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("SupplierDAOImpl.java"))
                .isDirectoryNotContaining(path -> path.getFileName().toString().equals("RequestDAOImpl.java"));
  
        mojo.bdmModelFile = new File(GenerateBdmModelSourceMojoTest.class.getResource("/anotherBom.xml").getFile());
        
        mojo.execute();
        
        assertThat(packageFolder)
            .isDirectoryContaining(path -> path.getFileName().toString().equals("Employee.java"))
            .isDirectoryNotContaining(path -> path.getFileName().toString().equals("Quotation.java"));
            
    }

    @Test
    void skipGenerationWhenNoDelta(@TempDir Path outputFolder) throws Exception {
        var mojo = new GenerateBdmModelSourceMojo(new BusinessDataModelParserImpl(),
                new BusinessDataModelGeneratorImpl(), buildContext);
        mojo.bdmModelFile = new File(GenerateBdmModelSourceMojoTest.class.getResource("/bom.xml").getFile());
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();
        when(buildContext.hasDelta(Mockito.any(String.class))).thenReturn(false);

        mojo.execute();

        assertThat(outputFolder).isEmptyDirectory();
    }

    @ParameterizedTest
    @MethodSource("provideFileForMissingDescriptor")
    void skipGenerationWhenNoDecriptorFound(File model, @TempDir Path outputFolder) throws Exception {
        var mojo = new GenerateBdmModelSourceMojo(new BusinessDataModelParserImpl(),
                new BusinessDataModelGeneratorImpl(), buildContext);
        mojo.bdmModelFile = model;
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();

        mojo.execute();

        assertThat(outputFolder).isEmptyDirectory();
    }

    @Test
    void throwMojoFailureExceptionWithInvalidModel(@TempDir Path outputFolder) throws Exception {
        var mojo = new GenerateBdmModelSourceMojo(new BusinessDataModelParserImpl(),
                new BusinessDataModelGeneratorImpl(), buildContext);
        mojo.bdmModelFile = new File(GenerateBdmModelSourceMojoTest.class.getResource("/invalidBom.xml").getFile());
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();

        assertThrows(MojoFailureException.class,
                () -> mojo.execute(),
                "Error while parsing the model descriptor");
    }
    
    @Test
    void throwMojoFailureExceptionWhenCodeGenFails(@TempDir Path outputFolder) throws Exception {
        var generator = mock( BusinessDataModelGenerator.class);
        var mojo = new GenerateBdmModelSourceMojo(new BusinessDataModelParserImpl(),
                generator, buildContext);
        File modelFile = new File(GenerateBdmModelSourceMojoTest.class.getResource("/bom.xml").getFile());
        mojo.bdmModelFile = modelFile;
        mojo.outputFolder = outputFolder.toFile();
        mojo.project = new MavenProject();
        
        doThrow(CodeGenerationException.class).when(generator)
            .generate(Mockito.any(BusinessObjectModel.class), Mockito.eq(outputFolder));

        org.junit.jupiter.api.Assertions.assertThrows(MojoFailureException.class,
                () -> mojo.execute(),
                "Error while generating bdm model sources");
    }

    private static Stream<Arguments> provideFileForMissingDescriptor() {
        return Stream.of(
                Arguments.of((File) null),
                Arguments.of(new File("notExisting.xml")));
    }

}
