/**
 * Copyright (C) 2023 BonitaSoft S.A.
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
package org.bonitasoft.plugin.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.bonitasoft.plugin.extension.impl.ExtensionsModuleGeneratorImpl;

public abstract class AbstractModuleGenerator implements ModuleGenerator {

    protected ModelReader modelReader;
    protected ModelWriter modelWriter;

    protected AbstractModuleGenerator(ModelReader modelReader, ModelWriter modelWriter) {
        this.modelReader = modelReader;
        this.modelWriter = modelWriter;
    }
    
    public Path createModule(String projectId, Model parent, Path moduleFolder, String templateFileName,
            String moduleNameSuffix) throws ModuleGenerationException {

        Path modulePom = moduleFolder.resolve(POM_FILE_NAME);
        modulePom.toFile().getParentFile().mkdirs();
        try (var is = ExtensionsModuleGeneratorImpl.class.getResourceAsStream(templateFileName);
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
