/**
 * 
 */
package org.bonitasoft.plugin.bdm.module;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.bdm.module.impl.BdmModuleGeneratorImpl;
import org.bonitasoft.plugin.bdm.module.impl.DefaultBomFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.plexus.build.incremental.BuildContext;

@ExtendWith(MockitoExtension.class)
class CreateBdmModuleMojoTest {

    private ModelReader reader = new DefaultModelReader();
    private ModelWriter writer = new DefaultModelWriter();
    @Mock
    private BuildContext context;

    @Test
    void throwErrorWhenProjectDoesNotHavePomPackaging() throws Exception {
        var mojo = new CreateBdmModuleMojo(new BdmModuleGeneratorImpl(reader, writer), new DefaultBomFactoryImpl(),
                context);
        var project = new MavenProject();
        project.setPackaging("jar");
        mojo.project = project;
        
        assertThrows(MojoExecutionException.class, () -> mojo.execute(), "The project must have a pom packaging.");
    }
    
}
