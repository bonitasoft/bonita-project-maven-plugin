/**
 * Copyright (C) 2022 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.bdm.module;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * This mojo create a bdm module and its submodules in the current project with a Business Object Model descriptor sample file.
 *
 */
@Mojo(name = "create-bdm-module", defaultPhase = LifecyclePhase.NONE)
public class CreateBdmModuleMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Specify the Bonita project id that will determine the artifact ids of the generated modules.
     * This id must be consistent with the parent project.
     */
    @Parameter(required = true, property = "bonitaProjectId")
    protected String bonitaProjectId;

    private BuildContext buildContext;
    private BdmModuleGenerator moduleGenerator;
    private DefaultBomFactory defaultBomFactory;

    @Inject
    public CreateBdmModuleMojo(BdmModuleGenerator moduleGenerator, DefaultBomFactory defaultBomProvider,
            BuildContext buildContext) {
        this.moduleGenerator = moduleGenerator;
        this.buildContext = buildContext;
        this.defaultBomFactory = defaultBomProvider;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!"pom".equals(project.getPackaging()) 
                || !Objects.equals(project.getArtifactId(), bonitaProjectId+"-parent")) {
          return;
        }
        var instant = Instant.now();
        getLog().info("Creating Business Data Model maven modules...");
        try {
            var modulePath = moduleGenerator.create(bonitaProjectId, project);
            if(!Files.exists(modulePath.resolve("bom.xml"))){
                defaultBomFactory.createDefaultBom(project.getGroupId(), modulePath);
            }
            buildContext.refresh(modulePath.toFile());
        } catch (ModuleGenerationException e) {
            throw new MojoFailureException("Error while generating the Business Data Model maven modules", e);
        } catch (IOException ioe) {
            throw new MojoFailureException("Error while generating the default Business Data Model file descriptor", ioe);
        }

        getLog().info(String.format("Business Data Model Maven modules generation completed in %s.",
                Duration.between(Instant.now(), instant)));
    }

}
