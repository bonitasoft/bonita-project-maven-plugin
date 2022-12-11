package org.bonitasoft.plugin.bdm.module.impl;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.validation.SchemaFactory;

import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.plugin.bdm.module.DefaultBomFactory;

@Singleton
@Named
public class DefaultBomFactoryImpl implements DefaultBomFactory {

    private static final String RESERVED_COM_PREFIX = "com.bonitasoft";
    private static final String RESERVED_ORG_PREFIX = "org.bonitasoft";
    static final String DEFAULT_PACKAGE_PREFIX = "com.company";
    static final String DEFAULT_BO_NAME = "BusinessObject";
    static final String DEFAULT_FIELD_NAME = "attribute";
    static final String BOM_FILE_NAME = "bom.xml";

    private static Marshaller marshaller;
    static {
        try {
            var schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(BusinessObjectModel.class.getResource("/bom.xsd"));
            var contextObj = JAXBContext.newInstance(BusinessObjectModel.class);
            marshaller = contextObj.createMarshaller();
            marshaller.setSchema(schema);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path createDefaultBom(String projectGroupId, Path modulePath) throws IOException {
        if (modulePath.resolve(BOM_FILE_NAME).toFile().exists()) {
            throw new FileAlreadyExistsException(
                    String.format("The %s for the module %s already exist for the project %s", BOM_FILE_NAME,
                            modulePath.getFileName(), projectGroupId));
        }

        var newBDM = new BusinessObjectModel();
        newBDM.getBusinessObjects().add(createFirstBusinessObject(projectGroupId));
        return marshall(newBDM, Files.createFile(modulePath.resolve(BOM_FILE_NAME)));
    }

    private static Path marshall(BusinessObjectModel newBDM, Path bomFile) throws IOException {
        try (var os = Files.newOutputStream(bomFile)) {
            marshaller.marshal(newBDM, os);
            return bomFile;
        } catch (JAXBException e) {
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
        if (!packagePrefix.matches("^[a-z_]+(\\.[a-z_][a-z0-9_]*)*$")) {
            packagePrefix = DEFAULT_PACKAGE_PREFIX;
        }
        return packagePrefix;
    }
}
