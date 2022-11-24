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
package org.bonitasoft.plugin.bdm.module.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.bdm.module.BdmModuleGenerator;
import org.bonitasoft.plugin.bdm.module.ModuleGenerationException;

@Named
public class BdmModuleGeneratorImpl implements BdmModuleGenerator {

    static final String BDM_PARENT_MODULE = "bdm";
    static final String BDM_PARENT_MODULE_SUFFIX = "-bdm-parent";
    static final String MODEL_MODULE_NAME = "model";
    static final String MODEL_MODULE_NAME_SUFFIX = "-bdm-model";
    static final String DAO_CLIENT_MODULE_NAME = "dao-client";
    static final String DAO_CLIENT_MODULE_NAME_SUFFIX = "-bdm-dao-client";
    static final String POM_FILE_NAME = "pom.xml";
    static final String BDM_MODEL_ARTIFACT_ID_PLACEHOLDER = "_BDM_MODEL_ARTIFACT_ID_PLACEHOLDER_";

    private ModelReader modelReader;
    private ModelWriter modelWriter;

    @Inject
    public BdmModuleGeneratorImpl(ModelReader modelReader, ModelWriter modelWriter) {
        this.modelReader = modelReader;
        this.modelWriter = modelWriter;
    }

    @Override
    public Path create(String projectId, MavenProject parentProject) throws ModuleGenerationException {
        var parentModuleFolder = createModule(projectId, parentProject.getModel(),
                parentProject.getBasedir().toPath().resolve(BDM_PARENT_MODULE), "/bdm.parent.module.xml",
                BDM_PARENT_MODULE_SUFFIX);
        try {
            var parentModel = modelReader.read(parentModuleFolder.resolve(POM_FILE_NAME).toFile(), null);
            parentModel.setGroupId(parentProject.getGroupId());
            parentModel.setVersion(parentProject.getVersion());
            createModule(projectId, parentModel, parentModuleFolder.resolve(MODEL_MODULE_NAME), "/bdm.model.module.xml",
                    MODEL_MODULE_NAME_SUFFIX);
            Path daoClientModule = createModule(projectId, parentModel,
                    parentModuleFolder.resolve(DAO_CLIENT_MODULE_NAME),
                    "/bdm.dao.module.xml", DAO_CLIENT_MODULE_NAME_SUFFIX);
            updateBdmModelDependency(daoClientModule, parentProject.getGroupId(), projectId + MODEL_MODULE_NAME_SUFFIX);
            var parentProjectModel = modelReader.read(parentProject.getFile(), null);
            if (parentProjectModel.getModules().stream().noneMatch(BDM_PARENT_MODULE::equals)) {
                parentProjectModel.getModules().add(BDM_PARENT_MODULE);
            }
            modelWriter.write(parentProject.getFile(), null, parentProjectModel);
        } catch (IOException e) {
            throw new ModuleGenerationException("Failed to add bdm module to parent pom.", e);
        }
        return parentModuleFolder;
    }

    private void updateBdmModelDependency(Path daoClientModule, String groupId, String bdmModelArtifactId)
            throws IOException {
        var pomFile = daoClientModule.resolve(POM_FILE_NAME).toFile();
        var model = modelReader.read(pomFile, null);
        var dep = model.getDependencies().stream()
                .filter(d -> BDM_MODEL_ARTIFACT_ID_PLACEHOLDER.equals(d.getArtifactId()))
                .findFirst()
                .orElseThrow();
        dep.setArtifactId(bdmModelArtifactId);
        modelWriter.write(pomFile, null, model);
    }

    Path createModule(String projectId, Model parent, Path moduleFolder, String templateFileName,
            String moduleNameSuffix) throws ModuleGenerationException {

        if (moduleFolder.toFile().exists()) {
            throw new ModuleGenerationException(String.format("The module %s already exist for the project %s",
                    moduleFolder.getFileName(), projectId));
        }

        Path modulePom = moduleFolder.resolve(POM_FILE_NAME);
        modulePom.toFile().getParentFile().mkdirs();
        try (var is = BdmModuleGeneratorImpl.class.getResourceAsStream(templateFileName);
                var os = Files.newOutputStream(modulePom)) {
            var modelTemplate = modelReader.read(is, null);
            modelTemplate.setArtifactId(projectId + moduleNameSuffix);
            var parentModel = modelTemplate.getParent();
            parentModel.setGroupId(parent.getGroupId());
            parentModel.setArtifactId(parent.getArtifactId());
            parentModel.setVersion(parent.getVersion());
            modelWriter.write(os, null, modelTemplate);
        } catch (IOException e) {
            throw new ModuleGenerationException(
                    String.format("Failed to write %s module pom.", moduleFolder.getFileName()), e);
        }
        return moduleFolder;
    }

}
