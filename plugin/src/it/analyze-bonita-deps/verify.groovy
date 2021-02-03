import java.io.*
import groovy.json.JsonSlurper

// Dependency from the project store should be installed in the local repository
File bonitaDependencyAnalisysOutputFile = new File( basedir, 'target/bonita-dependencies.json');
if ( !bonitaDependencyAnalisysOutputFile.isFile() ) {
    throw new FileNotFoundException( "Could not find analysis output file: " + bonitaDependencyAnalisysOutputFile );
}

def report = new JsonSlurper().parse(bonitaDependencyAnalisysOutputFile)

assert report.connectorImplementations.definitionId == ['email']
assert report.connectorImplementations.definitionVersion == ['1.2.0']
assert report.connectorImplementations.implementationId == ['email-impl']
assert report.connectorImplementations.implementationVersion == ['1.3.0']
assert report.connectorImplementations.className == ['org.bonitasoft.connectors.email.EmailConnector']
assert report.connectorImplementations.jarEntry == ['email.impl']
assert report.connectorImplementations.type == ['CONNECTOR']

assert report.filterDefinitions.definitionId == ['bonita-actorfilter-single-user']
assert report.filterDefinitions.definitionVersion == ['1.0.0']
assert report.filterDefinitions.jarEntry == ['bonita-actorfilter-single-user.def']
assert report.filterDefinitions[0].filePath.endsWith('bonita-actorfilter-single-user-1.0.0.jar')

