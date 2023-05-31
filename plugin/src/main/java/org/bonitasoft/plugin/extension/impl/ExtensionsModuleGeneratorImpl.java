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
package org.bonitasoft.plugin.extension.impl;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.bonitasoft.plugin.module.AbstractModuleGenerator;
import org.bonitasoft.plugin.module.ModuleGenerationException;

@Named
public class ExtensionsModuleGeneratorImpl extends AbstractModuleGenerator {

    static final String MODULE_SUFFIX = "-extensions";
    static final String EXTENSIONS_PARENT_MODULE = "extensions";
    static final String POM_FILE_NAME = "pom.xml";
    static final String BDM_MODEL_ARTIFACT_ID_PLACEHOLDER = "_BDM_MODEL_ARTIFACT_ID_PLACEHOLDER_";

    @Inject
    public ExtensionsModuleGeneratorImpl(ModelReader modelReader, ModelWriter modelWriter) {
        super(modelReader, modelWriter);
    }

    @Override
    public Path create(String projectId, MavenProject parentProject) throws ModuleGenerationException {
        var modulePath =  createModule(projectId, parentProject.getModel(),
                parentProject.getBasedir().toPath().resolve(EXTENSIONS_PARENT_MODULE), "/extensions.parent.module.xml",
                MODULE_SUFFIX);
        try {
           var parentProjectModel = modelReader.read(parentProject.getFile(), null);
           if (parentProjectModel.getModules().stream().noneMatch(EXTENSIONS_PARENT_MODULE::equals)) {
               parentProjectModel.getModules().add(EXTENSIONS_PARENT_MODULE);
           }
           modelWriter.write(parentProject.getFile(), null, parentProjectModel);
        } catch (IOException e) {
           throw new ModuleGenerationException("Failed to add extensions module to parent pom.", e);
        }
        
        return modulePath;
    }

  
   

}
