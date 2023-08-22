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
package org.bonitasoft.plugin.validation.xml;

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

import org.bonitasoft.plugin.validation.ValidationErrorException;
import org.bonitasoft.plugin.validation.ValidationException;
import org.bonitasoft.plugin.validation.ValidationTask;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to validate XML files located in a directory using a given XSD schema.
 */
@Slf4j
public class XmlValidationTask implements ValidationTask {

    protected static final String DEFAULT_TASK_NAME = "XML Validation Task";
    private static final String DEFAULT_SOURCE_FILE_REGEX = "^.*\\.xml$";

    private final String taskName;
    private final URL xsdUrl;
    private final Path artifactsSourceDir;
    private final String sourceFileRegex;

    public XmlValidationTask(String taskName, URL xsdUrl, Path artifactsSourceDir, String sourceFileRegex) {
        this.taskName = taskName;
        this.xsdUrl = xsdUrl;
        this.artifactsSourceDir = artifactsSourceDir;
        this.sourceFileRegex = sourceFileRegex == null ? DEFAULT_SOURCE_FILE_REGEX : sourceFileRegex;
    }

    public XmlValidationTask(String taskName, URL xsdUrl, Path artifactsSourceDir) {
        this(taskName, xsdUrl, artifactsSourceDir, DEFAULT_SOURCE_FILE_REGEX);
    }

    public XmlValidationTask(URL xsdUrl, Path artifactsSourceDir, String sourceFileRegex) {
        this(DEFAULT_TASK_NAME, xsdUrl, artifactsSourceDir, sourceFileRegex);
    }

    public XmlValidationTask(URL xsdUrl, Path artifactsSourceDir) {
        this(DEFAULT_TASK_NAME, xsdUrl, artifactsSourceDir);
    }

    @Override
    public void validate() throws ValidationException {
        List<File> sourceFiles = getSourceFiles();
        if (sourceFiles.isEmpty()) {
            // nothing to validate
            return;
        }
        log.info("Executing {}", taskName);
        Validator validator = initValidator();
        for (File file : sourceFiles) {
            try {
                log.debug("Executing validation on file [{}]", file.getName());
                validator.validate(new StreamSource(file));
                log.info("File '{}' is valid", file.getName());
            } catch (Exception e) {
                throw new ValidationException("File '" + file.getName() + "' is not valid", e);
            }
        }
    }

    protected Validator initValidator() throws ValidationErrorException {
        log.debug("Initializing schema validator [{}]", xsdUrl);
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            sf.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return sf.newSchema(xsdUrl).newValidator();
        } catch (Exception e) {
            throw new ValidationErrorException("[" + taskName + "] Failed to parse XSD with URL " + xsdUrl, e);
        }
    }

    protected List<File> getSourceFiles() throws ValidationErrorException {
        if (!Files.exists(artifactsSourceDir) || !Files.isDirectory(artifactsSourceDir)) {
            log.debug("Artifacts source directory [{}] does not exist or is not a directory", artifactsSourceDir);
            return Collections.emptyList();
        }
        try (Stream<Path> sourcePaths = Files.list(artifactsSourceDir)) {
            var sourceFiles = sourcePaths
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().matches(sourceFileRegex))
                    .map(Path::toFile)
                    .sorted()
                    .collect(Collectors.toList());
            log.debug("Found [{}] source files in directory [{}] matching regex [{}] for task [{}]", sourceFiles.size(),
                    artifactsSourceDir, sourceFileRegex, taskName);
            return sourceFiles;
        } catch (IOException e) {
            throw new ValidationErrorException(
                    "[" + taskName + "] Failed to list files in directory " + artifactsSourceDir, e);
        }
    }
}
