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

import java.time.Duration;
import java.time.Instant;

import javax.inject.Inject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "create-bdm-module", defaultPhase = LifecyclePhase.NONE)
public class CreateBdmModuleMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject project;

    @Parameter(required = true)
    protected String projectId;

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
        getLog().info("Creating Business Data Model maven modules...");
        var instant = Instant.now();
        try {
            var modulePath = moduleGenerator.createModule(projectId, project);
            var defaultBom = defaultBomFactory.createDefaultBom(project.getGroupId());
            buildContext.refresh(modulePath.toFile());
        } catch (ModuleGenerationException e) {
            throw new MojoFailureException("Error while generating the Business Data Model Mmaven modules", e);
        }

        getLog().info(String.format("Business Data Model Maven modules generation completed in %s.",
                Duration.between(Instant.now(), instant)));
    }

}
