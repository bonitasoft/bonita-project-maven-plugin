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
import org.bonitasoft.plugin.bdm.module.ModuleGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

class DefaultBomFactoryImplTest {

    private static final String PROCUREMENT_EXAMPLE_PROJECT_ID = "procurement-example";
    private static final String PROCUREMENT_EXAMPLE_GROUP_ID = "org.company.example";
    private ModelReader modelReader = new DefaultModelReader();
    private ModelWriter modelWriter = new DefaultModelWriter();

    @Test
    void createDefaultBom(@TempDir Path tmpDir) throws Exception {
        var returnedBom = createBom(tmpDir, PROCUREMENT_EXAMPLE_GROUP_ID);

        assertThat(returnedBom.getBusinessObjects().get(0).getQualifiedName())
                .isEqualTo(String.format("%s.model.%s", PROCUREMENT_EXAMPLE_GROUP_ID,
                        DefaultBomFactoryImpl.DEFAULT_BO_NAME));
    }

    @Test
    void useDefaultPackagePrefixWhenReservedOrgPrefixInGroupId(@TempDir Path tmpDir) throws Exception {
        var returnedBom = createBom(tmpDir, "org.bonitasoft.example");

        assertThat(returnedBom.getBusinessObjects().get(0).getQualifiedName())
                .isEqualTo(String.format("%s.model.%s", DefaultBomFactoryImpl.DEFAULT_PACKAGE_PREFIX,
                        DefaultBomFactoryImpl.DEFAULT_BO_NAME));
    }
    
    @Test
    void useDefaultPackagePrefixWhenReservedComPrefixInGroupId(@TempDir Path tmpDir) throws Exception {
        var returnedBom = createBom(tmpDir, "com.bonitasoft.example");

        assertThat(returnedBom.getBusinessObjects().get(0).getQualifiedName())
                .isEqualTo(String.format("%s.model.%s", DefaultBomFactoryImpl.DEFAULT_PACKAGE_PREFIX,
                        DefaultBomFactoryImpl.DEFAULT_BO_NAME));
    }

    @Test
    void useDefaultPackagePrefixWhenRGroupIdHasNotAPackageFormat(@TempDir Path tmpDir) throws Exception {
        var returnedBom = createBom(tmpDir, "company-group-id");
        
        assertThat(returnedBom.getBusinessObjects().get(0).getQualifiedName())
                .isEqualTo(String.format("%s.model.%s", DefaultBomFactoryImpl.DEFAULT_PACKAGE_PREFIX,
                        DefaultBomFactoryImpl.DEFAULT_BO_NAME));
    }

    @Test
    void throwFileAlreadyExistsExceptionWhenBomAlreadyExist(@TempDir Path tmpDir) throws Exception {
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
