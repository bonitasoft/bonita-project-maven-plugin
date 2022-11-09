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
package org.bonitasoft.plugin.builder.bdm.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Parent;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.builder.bdm.BdmModuleGenerator;
import org.bonitasoft.plugin.builder.bdm.ModuleGenerationException;

@Named
public class BdmModuleGeneratorImpl implements BdmModuleGenerator {

    private static final String BDM_PARENT_MODULE = "bdm";
    private static final String POM_FILE_NAME = "pom.xml";
    
    private ModelReader modelReader;
    private ModelWriter modelWriter;

    @Inject
    public BdmModuleGeneratorImpl(ModelReader modelReader, ModelWriter modelWriter) {
        this.modelReader = modelReader;
        this.modelWriter = modelWriter;
    }

    public void generate(File moduleFolder, MavenProject parentProject) throws ModuleGenerationException {
        if (!moduleFolder.exists()) {
            moduleFolder.mkdirs();
        }
        Path moduleParentPom = moduleFolder.toPath().resolve(POM_FILE_NAME);
        try (var is = BdmModuleGeneratorImpl.class.getResourceAsStream("/bdm.parent.module.xml");
                var os = Files.newOutputStream(moduleParentPom);) {
            var modelTemplate = modelReader.read(is, null);
            Parent parent = modelTemplate.getParent();
            parent.setGroupId(parentProject.getGroupId());
            parent.setArtifactId(parentProject.getArtifactId());
            parent.setVersion(parentProject.getVersion());
            modelWriter.write(os, null, modelTemplate);
        } catch (IOException e) {
            throw new ModuleGenerationException("Failed to write bdm module pom.", e);
        }
        Path moduleClientPom = moduleFolder.toPath().resolve("bdm-client").resolve(POM_FILE_NAME);
        moduleClientPom.toFile().getParentFile().mkdirs();
        try (var is = BdmModuleGeneratorImpl.class.getResourceAsStream("/bdm.client.module.xml");
                var os = Files.newOutputStream(moduleClientPom);) {
            var modelTemplate = modelReader.read(is, null);
            Parent parent = modelTemplate.getParent();
            parent.setGroupId(parentProject.getGroupId());
            parent.setVersion(parentProject.getVersion());
            modelWriter.write(os, null, modelTemplate);
        } catch (IOException e) {
            throw new ModuleGenerationException("Failed to write bdm-client module pom.", e);
        }
        Path moduleDaoPom = moduleFolder.toPath().resolve("bdm-dao").resolve(POM_FILE_NAME);
        moduleDaoPom.toFile().getParentFile().mkdirs();
        try (var is = BdmModuleGeneratorImpl.class.getResourceAsStream("/bdm.dao.module.xml");
                var os = Files.newOutputStream(moduleDaoPom);) {
            var modelTemplate = modelReader.read(is, null);
            Parent parent = modelTemplate.getParent();
            parent.setGroupId(parentProject.getGroupId());
            parent.setVersion(parentProject.getVersion());
            modelWriter.write(os, null, modelTemplate);
        } catch (IOException e) {
            throw new ModuleGenerationException("Failed to write bdm-dao module pom.", e);
        }
     
        try {
            var parentProjectModel = modelReader.read(parentProject.getFile(), null);
            if (parentProjectModel.getModules().stream().noneMatch(BDM_PARENT_MODULE::equals)) {
                parentProjectModel.getModules().add(BDM_PARENT_MODULE);
            }
            modelWriter.write(parentProject.getFile(), null, parentProjectModel);
        } catch (IOException e) {
            throw new ModuleGenerationException("Failed to add bdm module to parent pom.", e);
        }
    }

}
