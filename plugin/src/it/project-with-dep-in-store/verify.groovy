import java.io.*;

// Dependency from the project store should be installed in the local repository
File jarFile = new File( localRepositoryPath, 'org/bonitasoft/in-store/0.0.1-SNAPSHOT/in-store-0.0.1-SNAPSHOT.jar');
if ( !jarFile.isFile() ) {
    throw new FileNotFoundException( "Could not find installed JAR: " + jarFile );
}

File pomFile = new File( localRepositoryPath, 'org/bonitasoft/in-store/0.0.1-SNAPSHOT/in-store-0.0.1-SNAPSHOT.pom');
if ( !pomFile.isFile() ) {
    throw new FileNotFoundException( "Could not find installed pom: " + pomFile );
}