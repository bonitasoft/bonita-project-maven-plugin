import java.io.*;

import org.apache.maven.model.io.xpp3.MavenXpp3Reader

// Dependency from the project store should be installed in the local repository
File jarFile = new File( localRepo, 'org/bonitasoft/connectors/invalid-parent-artifact/1.3.0/invalid-parent-artifact-1.3.0.jar');
if ( !jarFile.isFile() ) {
    throw new FileNotFoundException( "Could not find installed JAR: " + jarFile );
}

File pomFile = new File( localRepo, 'org/bonitasoft/connectors/invalid-parent-artifact/1.3.0/invalid-parent-artifact-1.3.0.pom');
if ( !pomFile.isFile() ) {
    throw new FileNotFoundException( "Could not find installed pom: " + pomFile );
}

pomFile.withInputStream { 
  def model =  new MavenXpp3Reader().read(it)
  assert model.parent == null : 'The unresolvable parent should have been removed'
  assert model.groupId == 'org.bonitasoft.connectors'
  assert model.artifactId == 'invalid-parent-artifact'
  assert model.version == '1.3.0'
}
