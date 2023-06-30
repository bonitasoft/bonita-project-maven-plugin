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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.plugin.test.TestFiles.getResourceAsFile;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class JsonDependencyReportReporterTest extends AbstractDependencyReportReporterTest {

    protected JsonDependencyReporter reporter;
    protected File outputFile;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        outputFile = new File(tempDir, "report.json");
        reporter = new JsonDependencyReporter(outputFile);
    }

    @Override
    protected DependencyReporter getReporter() {
        return reporter;
    }

    @Override
    protected void assertReportIsValid() throws Exception {
        final File expectedContent = getResourceAsFile("/expected-report.json");
        assertThat(outputFile).exists().isFile().hasSameTextualContentAs(expectedContent, UTF_8);
    }

}
