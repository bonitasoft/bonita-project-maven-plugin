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

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import javax.inject.Named;

import org.bonitasoft.engine.bdm.BusinessObjectModelConverter;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.plugin.bdm.codegen.BusinessDataModelParser;
import org.bonitasoft.plugin.bdm.codegen.ParseException;

@Named
public class BusinessDataModelParserImpl implements BusinessDataModelParser {
    
    private BusinessObjectModelConverter converter = new BusinessObjectModelConverter();

    @Override
    public BusinessObjectModel parse(File modelFile) throws ParseException {
        var file = requireNonNull(modelFile, "modelFile cannot be null !");
        try {
            return converter.unmarshall(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new ParseException(String.format("Failed to parse %s.", modelFile), e);
        }
    }

}
