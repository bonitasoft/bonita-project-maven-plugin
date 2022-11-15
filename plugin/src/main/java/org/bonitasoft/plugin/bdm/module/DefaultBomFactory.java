package org.bonitasoft.plugin.bdm.module;

import java.nio.file.Path;

public interface DefaultBomFactory {

    Path createDefaultBom(String projectGroupId) throws ModuleGenerationException;

}
