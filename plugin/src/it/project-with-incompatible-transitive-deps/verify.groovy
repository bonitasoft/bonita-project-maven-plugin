import java.io.*
import groovy.json.JsonSlurper

File bonitaDependencyAnalisysOutputFile = new File( basedir, 'target/bonita-dependencies.json');
if ( !bonitaDependencyAnalisysOutputFile.isFile() ) {
    throw new FileNotFoundException( "Could not find analysis output file: " + bonitaDependencyAnalisysOutputFile );
}

def report = new JsonSlurper().parse(bonitaDependencyAnalisysOutputFile)

assert report.issues.size() == 2 : "Expected 2 issues but found ${report.issues.size()}"

assert report.issues.find { it.severity == 'ERROR' &&  it.message == 'org.bonitasoft.engine:bonita-server:jar:7.12.1 depends on org.codehaus.groovy:groovy-all:jar:2.4.20 which is conflicting with Bonita provided dependencies.'}
assert report.issues.find { it.severity == 'ERROR' &&  it.message == 'org.bonitasoft.engine:bonita-server:jar:7.12.1 is conflicting with Bonita provided dependencies.' }
