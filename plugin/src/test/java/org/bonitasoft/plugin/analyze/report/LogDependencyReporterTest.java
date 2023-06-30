/** 
 * Copyright (C) 2021 BonitaSoft S.A.
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

import static org.mockito.Mockito.verify;

import org.apache.maven.plugin.logging.Log;
import org.bonitasoft.plugin.analyze.report.model.DependencyReport;
import org.bonitasoft.plugin.analyze.report.model.Issue;
import org.bonitasoft.plugin.analyze.report.model.Issue.Severity;
import org.bonitasoft.plugin.analyze.report.model.Issue.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogDependencyReporterTest {

    @Mock
    private Log log;

    @Test
    void shouldLogWarnings() throws Exception {
        var reporter = new LogDependencyReporter(log);
        var report = new DependencyReport();
        report.addIssue(Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "hello", Severity.WARNING));

        reporter.report(report);

        verify(log).warn("hello");
    }

    @Test
    void shouldLogInfo() throws Exception {
        var reporter = new LogDependencyReporter(log);
        var report = new DependencyReport();
        report.addIssue(Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "hello", Severity.INFO));

        reporter.report(report);

        verify(log).info("hello");
    }

    @Test
    void shouldLogError() throws Exception {
        var reporter = new LogDependencyReporter(log);
        var report = new DependencyReport();
        report.addIssue(Issue.create(Type.INCOMPATIBLE_DEPENDENCY, "hello", Severity.ERROR));

        reporter.report(report);

        verify(log).error("hello");
    }

}
