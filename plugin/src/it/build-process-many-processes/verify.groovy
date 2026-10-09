import java.io.*
import java.util.zip.ZipFile
import groovy.io.FileType

def outputProcesses = new File(basedir, 'app/target/processes')
def businessArchives = []
outputProcesses.eachFileRecurse(FileType.FILES) {
    businessArchives << it
}

def expectedBarNames = (0..9).collect { "Pool${it}--1.0.bar".toString() }
assert businessArchives.name.containsAll(expectedBarNames): "Some expected business archives are missing from the output folder: found ${businessArchives.name}"

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

// Every process shares the same project-level dependencies (no per-process override here).
// Check all 10 bars, not just a sample: they're already built, so asserting on all of them is free,
// and it also catches a bug that would only surface on a subset of the repeated per-process resolver calls.
//
// Note on the SNAPSHOT case: bpa834-testlib is installed straight into the isolated local-repo (no remote
// repository / timestamped-SNAPSHOT resolution involved), so the resolved artifact's version and its local
// file name coincide here - this check mainly guards the verbatim-file-name code path in a real end-to-end
// run. The deeper "resolved version diverges from a hand-reconstructed name" case is covered at the unit
// level (InProcessDependencyCopierTest#should_copy_snapshot_dependency_with_exact_resolved_file_name).
(0..9).each { i ->
    def barName = "Pool${i}--1.0.bar".toString()
    def classpath = classpathOf(barName)
    assert classpath.contains('commons-lang3-3.12.0.jar'):
        "${barName} should embed the regular release dependency, got ${classpath}"
    assert classpath.contains('bpa834-testlib-1.0.0-SNAPSHOT.jar'):
        "${barName} should embed the exact SNAPSHOT jar file name, got ${classpath}"
    assert classpath.contains('bpa834-testlib-1.0.0-SNAPSHOT-impl.jar'):
        "${barName} should embed the exact classified jar file name, got ${classpath}"
}

true
