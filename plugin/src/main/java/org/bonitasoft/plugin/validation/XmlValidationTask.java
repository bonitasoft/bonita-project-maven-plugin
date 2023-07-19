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
package org.bonitasoft.plugin.validation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to validate XML files located in a directory using a given XSD schema.
 */
@Slf4j
public class XmlValidationTask implements ValidationTask {

    private static final String DEFAULT_SOURCE_FILE_REGEX = "^.*\\.xml$";

    private final URL xsdUrl;
    private final Path projectSourceDir;
    private final String sourceFileRegex;
    private final Validator validator;

    public XmlValidationTask(URL xsdUrl, Path projectSourceDir, String sourceFileRegex) {
        this.xsdUrl = xsdUrl;
        this.projectSourceDir = projectSourceDir;
        this.sourceFileRegex = sourceFileRegex;
        validator = initValidator();
    }

    public XmlValidationTask(URL xsdUrl, Path projectSourceDir) {
        this(xsdUrl, projectSourceDir, DEFAULT_SOURCE_FILE_REGEX);
    }

    @Override
    public void validate() throws ValidationException {
        for (File file : getSourceFiles()) {
            try {
                log.debug("Executing validation on file [{}]", file.getName());
                validator.validate(new StreamSource(file));
                log.info("File '{}' is valid", file.getName());
            } catch (Exception e) {
                throw new ValidationException("File '" + file.getName() + "' is not valid", e);
            }
        }
    }

    private Validator initValidator() {
        log.debug("Initializing schema validator [{}]", xsdUrl);
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return sf.newSchema(xsdUrl).newValidator();
        } catch (Exception e) {
            throw new ValidationErrorException("Failed to parse XSD with URL " + xsdUrl, e);
        }
    }

    protected List<File> getSourceFiles() {
        if (Files.exists(projectSourceDir) && Files.isDirectory(projectSourceDir)) {
            try (Stream<Path> sourcePaths = Files.list(projectSourceDir)) {
                var sourceFiles = sourcePaths
                        .filter(path -> Files.isRegularFile(path)
                                && path.getFileName().toString().matches(sourceFileRegex))
                        .map(Path::toFile).collect(Collectors.toList());
                log.debug("Found [{}] source files in directory [{}]", sourceFiles.size(), projectSourceDir);
                return sourceFiles;
            } catch (IOException e) {
                throw new ValidationErrorException("Failed to list files in directory " + projectSourceDir, e);
            }
        }
        log.debug("Project source directory [{}] does not exist or is not a directory", projectSourceDir);
        return Collections.emptyList();
    }
}
