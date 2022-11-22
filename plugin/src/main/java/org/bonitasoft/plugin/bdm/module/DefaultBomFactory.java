package org.bonitasoft.plugin.bdm.module;

import java.io.IOException;
import java.nio.file.Path;

public interface DefaultBomFactory {

    Path createDefaultBom(String projectGroupId, Path modulePath) throws IOException;

}
