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
package org.bonitasoft.plugin.analyze.handler;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipFile;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.CustomPage.CustomPageType;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CustomPageAnalyzer extends AbstractArtifactAnalyzerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomPageAnalyzer.class);
    private static final String CUSTOMPAGE_DESCRIPTOR_PROPERTIES = "page.properties";

    CustomPageAnalyzer(LocalRepositoryManager localRepositoryManager) {
        super(localRepositoryManager);
    }

    @Override
    public boolean appliesTo(Artifact artifact) {
        File file = artifact.getFile();
        var fileName = file.getName();
        try {
            return file.isFile() && fileName.endsWith(".zip") && hasCustomPageDescriptor(file);
        } catch (IOException e) {
            LOGGER.warn("An error occured while reading {}", file, e);
            return false;
        }
    }

    @Override
    public DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException {
        var properties = readPagePropertiesInArchive(artifact.getFile());
        analyzeCustomPageArtifact(artifact, properties, report);
        return report;
    }

    Properties readPagePropertiesInArchive(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().equals(CUSTOMPAGE_DESCRIPTOR_PROPERTIES))
                .map(entry -> {
                    try (ZipFile zipFile = new ZipFile(artifactFile);
                            Reader reader = new InputStreamReader(zipFile.getInputStream(entry),
                                    StandardCharsets.UTF_8)) {
                        Properties prop = new Properties();
                        prop.load(reader);
                        return prop;
                    } catch (IOException e) {
                        return null;
                    }
                }).filter(Objects::nonNull).orElseThrow(
                        () -> new IllegalArgumentException(format("No page.properties found in %s", artifactFile)));
    }

    void analyzeCustomPageArtifact(Artifact artifact, Properties pageDescriptor, DependencyReport result) {
        String contentType = pageDescriptor.getProperty("contentType");
        CustomPageType customPageType = CustomPageType.valueOf(contentType.toUpperCase());
        String name = pageDescriptor.getProperty(CustomPage.NAME_PROPERTY);
        String displayName = pageDescriptor.getProperty(CustomPage.DISPLAY_NAME_PROPERTY);
        String description = pageDescriptor.getProperty(CustomPage.DESCRIPTION_PROPERTY);
        switch (customPageType) {
            case FORM:
                result.addForm(Form.create(name, displayName, description, create(artifact)));
                break;
            case PAGE:
                result.addPage(Page.create(name, displayName, description, create(artifact)));
                break;
            case THEME:
                result.addTheme(Theme.create(name, displayName, description, create(artifact)));
                break;
            case APIEXTENSION:
                result.addRestAPIExtension(RestAPIExtension.create(name, displayName, description, create(artifact)));
                break;
            default:
                throw new AnalysisResultReportException("Unsupported Custom Page type: " + contentType);
        }
    }

    boolean hasCustomPageDescriptor(File artifactFile) throws IOException {
        return findZipEntry(artifactFile, entry -> entry.getName().equals(CUSTOMPAGE_DESCRIPTOR_PROPERTIES))
                .isPresent();
    }
}
