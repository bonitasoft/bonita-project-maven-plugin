import java.io.*
import groovy.json.JsonSlurper

// Dependency from the project store should be installed in the local repository
File bonitaDependencyAnalisysOutputFile = new File( basedir, 'app/target/bonita-dependencies.json');
if ( !bonitaDependencyAnalisysOutputFile.isFile() ) {
    throw new FileNotFoundException( "Could not find analysis output file: " + bonitaDependencyAnalisysOutputFile );
}

def report = new JsonSlurper().parse(bonitaDependencyAnalisysOutputFile)

def emailImpl = report.connectorImplementations.find{ it.definitionId == 'email' }

assert emailImpl.definitionId == 'email'
assert emailImpl.definitionVersion == '1.2.0'
assert emailImpl.implementationId == 'email-impl'
assert emailImpl.implementationVersion == '1.3.0'
assert emailImpl.className == 'org.bonitasoft.connectors.email.EmailConnector'
assert emailImpl.jarEntry == 'email.impl'
assert emailImpl.type ==  'CONNECTOR'
assert emailImpl.artifact.groupId ==  'org.bonitasoft.connectors'
assert emailImpl.artifact.artifactId ==  'bonita-connector-email'
assert emailImpl.artifact.version ==  '1.3.0'

assert report.filterDefinitions.definitionId == ['bonita-actorfilter-single-user']
assert report.filterDefinitions.definitionVersion == ['1.0.0']
assert report.filterDefinitions.jarEntry == ['bonita-actorfilter-single-user.def']
assert report.filterDefinitions[0].artifact.file.endsWith('bonita-actorfilter-single-user-1.0.0.jar')

assert report.restApiExtensions.name == ['custompage_myRestApiExtension']
assert report.restApiExtensions[0].artifact.file.endsWith('com/company/example/my-rest-api/1.0.0-SNAPSHOT/my-rest-api-1.0.0-SNAPSHOT.zip')

assert report.themes.name == ['custompage_myTheme']
assert report.themes.displayName == ['My custom Theme']
assert report.themes[0].artifact.file.endsWith('com/company/example/my-theme/1.0.0-SNAPSHOT/my-theme-1.0.0-SNAPSHOT.zip')

