/** 
 * Copyright (C) 2022 BonitaSoft S.A.
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
package org.bonitasoft.plugin.bdm.codegen;

import java.nio.file.Path;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * This mojo generates the BDM dao client java source code from the given Business Object Model descriptor file.
 */
@Mojo(name = "generate-bdm-dao-client", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GenerateBdmDaoClientSourceMojo extends AbstractGenerateBdmMojo {

    @Inject
    public GenerateBdmDaoClientSourceMojo(BusinessDataModelParser businessDataModelReader,
            BusinessDataModelGenerator generator, BuildContext buildContext) {
        super(businessDataModelReader, generator, buildContext);
    }

    @Override
    protected Predicate<Path> exludedGeneratedSources() {
        return file -> {
            String fileName = file.getFileName().toString();
            return fileName.endsWith(".java") && !fileName.endsWith("DAOImpl.java");
        };
    }

}
