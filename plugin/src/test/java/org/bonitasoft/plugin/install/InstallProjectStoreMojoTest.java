/**
 * Copyright (C) 2021 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.plugin.install;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InstallProjectStoreMojoTest {
    
    @Test
    void should_return_file_extension() throws Exception {
        String extension = InstallProjectStoreMojo.getExtension("lib.jar");
        
        assertThat(extension).isEqualTo("jar");
    }
    
    @Test
    void should_return_null_file_extension_when_name_ending_with_a_dot() throws Exception {
        String extension = InstallProjectStoreMojo.getExtension("lib.");
        
        assertThat(extension).isNull();
    }
    
    @Test
    void should_return_null_file_extension() throws Exception {
        String extension = InstallProjectStoreMojo.getExtension("lib");
        
        assertThat(extension).isNull();
    }

}
