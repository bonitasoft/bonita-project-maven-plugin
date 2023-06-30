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
package org.bonitasoft.plugin.bdm.codegen.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.bonitasoft.plugin.bdm.codegen.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BusinessDataModelParserImplTest {

    @Test
    void readBomFile() throws Exception {
        var bomFile = new File(BusinessDataModelParserImplTest.class.getResource("/bom.xml").getFile());
        var reader = new BusinessDataModelParserImpl();

        var model = reader.parse(bomFile);

        assertThat(model).isNotNull();
        assertThat(model.getBusinessObjectsClassNames())
                .contains("com.company.model.Quotation", "com.company.model.Request", "com.company.model.Supplier");
    }

    @Test
    void throwNPEWhenFileIsNull() throws Exception {
        var reader = new BusinessDataModelParserImpl();

        assertThrows(NullPointerException.class, () -> reader.parse(null), "modelFile cannot be null !");
    }

    @Test
    void throwUncheckedIOExceptionWhenFileDoesNotExist(@TempDir Path tmpDir) throws Exception {
        var file = tmpDir.resolve("notExisting.xml").toFile();

        var reader = new BusinessDataModelParserImpl();

        assertThrows(UncheckedIOException.class, () -> reader.parse(file));
    }

    @Test
    void throwParseExceptionWhenFileIsNotAValidBusinessObjectModel() throws Exception {
        var bomFile = new File(BusinessDataModelParserImplTest.class.getResource("/invalidBom.xml").getFile());

        var reader = new BusinessDataModelParserImpl();

        assertThrows(ParseException.class, () -> reader.parse(bomFile));
    }

}
