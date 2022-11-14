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
package org.bonitasoft.plugin.bdm.codegen.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.plugin.bdm.codegen.CodeGenerationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BusinessDataModelGeneratorImplTest {

    @Test
    void generateBusinessDataModel(@TempDir Path outputFolder) throws Exception {
        var model = createSimpleBusinessDataModel();

        var generator = new BusinessDataModelGeneratorImpl();

        generator.generate(model, outputFolder);

        assertThat(outputFolder).exists();
        var generatedModelSourceFile = outputFolder
                .resolve("org")
                .resolve("company")
                .resolve("MyObject.java");
        assertThat(generatedModelSourceFile).exists();
        var generatedDaoSourceFile = outputFolder
                .resolve("org")
                .resolve("company")
                .resolve("MyObjectDAO.java");
        assertThat(generatedDaoSourceFile).exists();
    }

    @Test
    void throwGenerationExceptionWhenEmptyModel(@TempDir Path outputFolder) throws Exception {
        var model = new BusinessObjectModel();

        var generator = new BusinessDataModelGeneratorImpl();

        assertThrows(CodeGenerationException.class, () -> generator.generate(model, outputFolder));
    }

    @Test
    void throwGenerationExceptionWithInvalidOutputFolder(@TempDir Path tmpDir) throws Exception {
        var model = createSimpleBusinessDataModel();
        var outputFolder = Files.createFile(tmpDir.resolve("outputFolder"));
        var generator = new BusinessDataModelGeneratorImpl();

        assertThrows(CodeGenerationException.class, () -> generator.generate(model, outputFolder),
                String.format("Output folder %s must be a directory", outputFolder));
    }

    private static BusinessObjectModel createSimpleBusinessDataModel() {
        var model = new BusinessObjectModel();
        var myBusinessObject = new BusinessObject("org.company.MyObject");
        var myField = new SimpleField();
        myField.setName("myField");
        myField.setType(FieldType.TEXT);
        myBusinessObject.addField(myField);
        model.addBusinessObject(myBusinessObject);
        return model;
    }
}
