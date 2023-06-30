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

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

class LogDependencyReportReporterTest extends AbstractDependencyReportReporterTest {

    private StringLogger logger;

    @Override
    protected DependencyReporter getReporter() {
        logger = new StringLogger();
        return new LogDependencyReporter(new DefaultLog(logger));
    }

    @Override
    protected void assertReportIsValid() throws Exception {
        String expectedLog = new String(getClass().getResourceAsStream("/expected-report.log").readAllBytes());
        assertThat(logger.getContent()).isEqualTo(expectedLog);
    }

    public static class StringLogger extends AbstractLogger {

        private StringBuilder builder = new StringBuilder();

        public StringLogger() {
            super(1, "test");
        }

        public String getContent() {
            return builder.toString();
        }

        @Override
        public void debug(String s, Throwable throwable) {
            builder.append("[DEBUG] ").append(s);
            addExceptionIfAny(throwable);
            builder.append(System.lineSeparator());
        }

        @Override
        public void info(String s, Throwable throwable) {
            builder.append("[INFO] ").append(s);
            addExceptionIfAny(throwable);
            builder.append(System.lineSeparator());
        }

        @Override
        public void warn(String s, Throwable throwable) {
            builder.append("[WARN] ").append(s);
            addExceptionIfAny(throwable);
            builder.append(System.lineSeparator());
        }

        @Override
        public void error(String s, Throwable throwable) {
            builder.append("[ERROR] ").append(s);
            addExceptionIfAny(throwable);
            builder.append(System.lineSeparator());
        }

        @Override
        public void fatalError(String s, Throwable throwable) {
            builder.append("[FATAL] ").append(s);
            addExceptionIfAny(throwable);
            builder.append(System.lineSeparator());
        }

        @Override
        public Logger getChildLogger(String s) {
            return null;
        }

        private void addExceptionIfAny(Throwable throwable) {
            builder.append(ofNullable(throwable).map(Throwable::getMessage).orElse(""));
        }
    }

}
