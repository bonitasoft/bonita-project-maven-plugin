import java.io.*;

// Dependency from the project store should be installed in the local repository
File bonitaDependencyAnalisysOutputFile = new File( basedir, 'target/bonita-dependencies.csv');
if ( !bonitaDependencyAnalisysOutputFile.isFile() ) {
    throw new FileNotFoundException( "Could not find analysis output file: " + bonitaDependencyAnalisysOutputFile );
}

def content = bonitaDependencyAnalisysOutputFile.text
assert content.contains('CONNECTOR_IMPLEMENTATION,email-impl,1.3.0,org.bonitasoft.connectors.email.EmailConnector,email,1.2.0,');
assert content.contains('FILTER_DEFINITION,bonita-actorfilter-single-user,1.0.0,');