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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Predicate;

import org.apache.maven.artifact.Artifact;
import org.bonitasoft.plugin.analyze.content.ArtifactContentReader;
import org.bonitasoft.plugin.analyze.report.AnalysisResultReportException;
import org.bonitasoft.plugin.analyze.report.model.CustomPage;
import org.bonitasoft.plugin.analyze.report.model.CustomPage.CustomPageType;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Form;
import org.bonitasoft.plugin.analyze.report.model.Page;
import org.bonitasoft.plugin.analyze.report.model.RestAPIExtension;
import org.bonitasoft.plugin.analyze.report.model.Theme;
import org.eclipse.aether.repository.LocalRepositoryManager;

class CustomPageAnalyzer extends AbstractArtifactAnalyzerHandler {

    private static final String CUSTOMPAGE_DESCRIPTOR_PROPERTIES = "page.properties";

    CustomPageAnalyzer(LocalRepositoryManager localRepositoryManager, ArtifactContentReader contentReader) {
        super(localRepositoryManager, contentReader);
    }

    @Override
    public boolean appliesTo(Artifact artifact) {
        return super.appliesTo(artifact) && hasCustomPageDescriptor(artifact);
    }

    @Override
    public DependencyReport analyze(Artifact artifact, DependencyReport report) throws IOException {
        var properties = readPageProperties(artifact);
        analyzeCustomPageArtifact(artifact, properties, report);
        return report;
    }

    Properties readPageProperties(Artifact artifact) throws IOException {
        Predicate<Path> isPageProperties = path -> path.getFileName().toString()
                .equals(CUSTOMPAGE_DESCRIPTOR_PROPERTIES);
        var result = getContentReader().readFirstEntry(artifact, isPageProperties, entry -> {
            try (var reader = new InputStreamReader(entry.supplier().get(), StandardCharsets.UTF_8)) {
                Properties prop = new Properties();
                prop.load(reader);
                return prop;
            } catch (IOException e) {
                getContentReader().logIOException(e, artifact.getFile(), entry.path());
                return null;
            }
        });
        return result.filter(Objects::nonNull).orElseThrow(
                () -> new IllegalArgumentException(format("No page.properties found in %s", artifact.getFile())));
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

    boolean hasCustomPageDescriptor(Artifact artifact) {
        return getContentReader().findEntryWithName(artifact, CUSTOMPAGE_DESCRIPTOR_PROPERTIES).isPresent();
    }
}
