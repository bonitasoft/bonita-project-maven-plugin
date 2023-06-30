/** 
 * Copyright (C) 2022 BonitaSoft S.A.
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
package org.bonitasoft.plugin.extension;

import java.util.Objects;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.extension.impl.ExtensionsModuleGeneratorImpl;
import org.bonitasoft.plugin.module.ModuleGenerationException;
import org.bonitasoft.plugin.module.ModuleGenerator;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * This mojo create an extensions module in the current project.
 */
@Mojo(name = "create-extensions-module", defaultPhase = LifecyclePhase.NONE)
public class CreateExtensionsModuleMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    /**
     * Specify the Bonita project id that will determine the artifact ids of the generated modules.
     * This id must be consistent with the parent project.
     */
    @Parameter(required = true, property = "bonitaProjectId")
    protected String bonitaProjectId;

    private BuildContext buildContext;
    private ModuleGenerator moduleGenerator;

    @Inject
    public CreateExtensionsModuleMojo(ExtensionsModuleGeneratorImpl moduleGenerator, BuildContext buildContext) {
        this.moduleGenerator = moduleGenerator;
        this.buildContext = buildContext;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!"pom".equals(project.getPackaging())
                || !Objects.equals(project.getArtifactId(), bonitaProjectId + "-parent")) {
            return;
        }
        getLog().info("Creating extensions maven module...");
        try {
            moduleGenerator.create(bonitaProjectId, project);
            buildContext.refresh(project.getFile());
        } catch (ModuleGenerationException ioe) {
            throw new MojoFailureException("Error while creating the extensions module.", ioe);
        }
    }

}
