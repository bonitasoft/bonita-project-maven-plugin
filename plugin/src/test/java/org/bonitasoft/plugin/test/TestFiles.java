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
package org.bonitasoft.plugin.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.bonitasoft.bonita2bar.configuration.ZipUtil;

public final class TestFiles {

    private TestFiles() {
        // Utility class
    }

    public static File getResourceAsFile(String filePath) throws URISyntaxException {
        return Paths.get(TestFiles.class.getResource(filePath).toURI()).toFile();
    }

    public static File getResourceAsZipFile(String folderPath) throws URISyntaxException, IOException {
        var folder = Paths.get(TestFiles.class.getResource(folderPath).toURI());
        // now make a temp zip with this folder
        var tempZip = File.createTempFile(folder.getFileName().toString(), ".zip");
        ZipUtil.zip(folder, tempZip.toPath());
        return tempZip;
    }
}
