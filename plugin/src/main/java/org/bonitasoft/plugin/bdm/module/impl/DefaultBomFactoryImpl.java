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
package org.bonitasoft.plugin.bdm.module.impl;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;

import org.bonitasoft.engine.bdm.BusinessObjectModelConverter;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.plugin.bdm.module.DefaultBomFactory;
import org.xml.sax.SAXException;

@Singleton
@Named
public class DefaultBomFactoryImpl implements DefaultBomFactory {

    private static final String RESERVED_COM_PREFIX = "com.bonitasoft";
    private static final String RESERVED_ORG_PREFIX = "org.bonitasoft";
    static final String DEFAULT_PACKAGE_PREFIX = "com.company";
    static final String DEFAULT_BO_NAME = "BusinessObject";
    static final String DEFAULT_FIELD_NAME = "attribute";
    static final String BOM_FILE_NAME = "bom.xml";

    private BusinessObjectModelConverter converter = new BusinessObjectModelConverter();

    @Override
    public Path createDefaultBom(String projectGroupId, Path modulePath) throws IOException {
        if (modulePath.resolve(BOM_FILE_NAME).toFile().exists()) {
            throw new FileAlreadyExistsException(
                    String.format("The %s for the module %s already exist for the project %s", BOM_FILE_NAME,
                            modulePath.getFileName(), projectGroupId));
        }

        var newBDM = new BusinessObjectModel();
        newBDM.getBusinessObjects().add(createFirstBusinessObject(projectGroupId));
        try {
            var bomFile = Files.createFile(modulePath.resolve(BOM_FILE_NAME));
            Files.write(bomFile, converter.marshall(newBDM));
            return bomFile;
        } catch (JAXBException | SAXException e) {
            throw new IOException(e);
        }
    }

    private BusinessObject createFirstBusinessObject(String projectGroupId) {
        var packagePrefix = toValidPackagePrefix(projectGroupId);
        String defaultName = String.format("%s.model.%s", packagePrefix,
                DEFAULT_BO_NAME);
        var bo = new BusinessObject(defaultName);
        SimpleField stringField = new SimpleField();
        stringField.setType(FieldType.STRING);
        stringField.setName(DEFAULT_FIELD_NAME);
        stringField.setLength(255);
        bo.addField(stringField);
        return bo;
    }

    private String toValidPackagePrefix(String projectGroupId) {
        var packagePrefix = projectGroupId;
        if (packagePrefix.startsWith(RESERVED_ORG_PREFIX) || packagePrefix.startsWith(RESERVED_COM_PREFIX)) {
            packagePrefix = DEFAULT_PACKAGE_PREFIX;
        }
        // Regexp validating package name format
        if (!packagePrefix.matches("^[a-z]+(\\.[a-z0-9]+).*$")) {
            packagePrefix = DEFAULT_PACKAGE_PREFIX;
        }
        return packagePrefix;
    }
}
