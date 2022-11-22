package org.bonitasoft.plugin.bdm.module.impl;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBException;

import org.bonitasoft.engine.bdm.BusinessObjectModelConverter;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.plugin.bdm.module.DefaultBomFactory;
import org.xml.sax.SAXException;

public class DefaultBomFactoryImpl implements DefaultBomFactory {

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
        String defaultName = String.format("%s.model.%s", projectGroupId,
                DEFAULT_BO_NAME);
        var bo = new BusinessObject(defaultName);
        SimpleField stringField = new SimpleField();
        stringField.setType(FieldType.STRING);
        stringField.setName(DEFAULT_FIELD_NAME);
        stringField.setLength(255);
        bo.addField(stringField);
        return bo;
    }
}
