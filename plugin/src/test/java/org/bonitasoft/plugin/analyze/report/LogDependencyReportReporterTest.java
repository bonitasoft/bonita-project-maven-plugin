package org.bonitasoft.plugin.analyze.report;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

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
