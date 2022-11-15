package org.bonitasoft.plugin.bdm.module.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BdmModuleGeneratorImplTest {

    private ModelReader modelReader = new DefaultModelReader();
    private ModelWriter modelWriter = new DefaultModelWriter();

    @Test
    void generateBdmModule(@TempDir Path tmpDir) throws Exception {
        var parentPomTemp = tmpDir.resolve("pom.xml");
        MavenProject mavenProject = new MavenProject();
        var parentPom = new File(BdmModuleGeneratorImplTest.class.getResource("/parentPom.xml").getFile());
        Files.copy(parentPom.toPath(), parentPomTemp);
        mavenProject.setFile(parentPomTemp.toFile());
        var generator = new BdmModuleGeneratorImpl(modelReader, modelWriter);

        generator.createModule("procurement-example", mavenProject);

        assertThat(tmpDir.resolve("bdm")).exists().isDirectoryContaining(path -> path.getFileName().toString().equals("pom.xml"));
        assertThat(tmpDir.resolve("bdm").resolve("model")).exists().isDirectoryContaining(path -> path.getFileName().toString().equals("pom.xml"));
        assertThat(tmpDir.resolve("bdm").resolve("dao-client")).exists().isDirectoryContaining(path -> path.getFileName().toString().equals("pom.xml"));
    }
}