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
