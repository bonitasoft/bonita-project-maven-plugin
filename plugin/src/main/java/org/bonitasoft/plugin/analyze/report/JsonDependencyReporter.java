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
package org.bonitasoft.plugin.analyze.report;

import java.io.File;
import java.io.IOException;

import org.bonitasoft.plugin.analyze.report.model.DependencyReport;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonDependencyReporter implements DependencyReporter {

    private final ObjectMapper mapper = DependencyReporter.objectMapper();

    private final File outputFile;

    public JsonDependencyReporter(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void report(DependencyReport dependencyReport) {
        try {
            final File parentFile = outputFile.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            mapper.writeValue(outputFile, dependencyReport);
        } catch (IOException e) {
            throw new AnalysisResultReportException("Failed to generate report", e);
        }
    }
}
