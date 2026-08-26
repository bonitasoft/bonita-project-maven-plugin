import java.io.*
import java.util.zip.ZipFile
import groovy.io.FileType

def outputProcesses = new File( basedir, 'app/target/processes');
def businessArchives = []
 outputProcesses.eachFileRecurse (FileType.FILES) {
     businessArchives << it
}

assert businessArchives.name.containsAll(['Pool--1.0.bar',
                           'Pool1--1.0.bar']): 'Some expected business archives are missing from the output folder'
assert  new File( basedir, 'app/target/build-process-project-1.0.0-SNAPSHOT-local.bconf').exists() : 'A Bonita Configuration file is missing in the output folder';

// Jars embedded in a business archive, by simple name
def classpathOf = { barName ->
    def bar = businessArchives.find { it.name == barName }
    def zip = new ZipFile(bar)
    try {
        return zip.entries().findAll { it.name.startsWith('classpath/') }
                            .collect { it.name.substring('classpath/'.length()) }
    } finally {
        zip.close()
    }
}

// Pool selects commons-text-1.12.0 in its configuration, while the project resolves 1.9:
// the selection must drive the resolution, and only the selected library is embedded.
def poolClasspath = classpathOf('Pool--1.0.bar')
assert poolClasspath.contains('commons-text-1.12.0.jar') : "Pool--1.0.bar should embed the selected commons-text version, got ${poolClasspath}"
assert !poolClasspath.any { it.startsWith('commons-text-1.9') } : "Pool--1.0.bar should not embed the version resolved by default, got ${poolClasspath}"

// Pool1 declares no dependency in its configuration: the project resolution applies, untouched.
def pool1Classpath = classpathOf('Pool1--1.0.bar')
assert pool1Classpath.contains('commons-text-1.9.jar') : "Pool1--1.0.bar should embed the version resolved by the project, got ${pool1Classpath}"
