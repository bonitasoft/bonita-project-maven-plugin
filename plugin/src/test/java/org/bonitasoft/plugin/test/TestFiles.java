package org.bonitasoft.plugin.test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public final class TestFiles {

	private TestFiles() {
		// Utility class
	}

	public static File getResourceAsFile(String filePath) throws URISyntaxException {
		return Paths.get(TestFiles.class.getResource(filePath).toURI()).toFile();
	}
}
