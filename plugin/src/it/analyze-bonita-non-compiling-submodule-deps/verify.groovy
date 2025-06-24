import groovy.json.JsonSlurper
import java.nio.file.Paths

// Dependency from the project store should be installed in the local repository
File bonitaDependencyAnalysisOutputFile = new File(basedir, 'app/target/bonita-dependencies.json');
if (!bonitaDependencyAnalysisOutputFile.isFile()) {
    throw new FileNotFoundException("Could not find analysis output file: " + bonitaDependencyAnalysisOutputFile);
}

def report = new JsonSlurper().parse(bonitaDependencyAnalysisOutputFile)

// although the connector does not compile, analysis is still done
def emailImpl = report.connectorImplementations.find{ it.definitionId == 'email' }
assert emailImpl != null

// compilation error reported
def compilationIssue = report.issues.find { it.type == 'EXTENSION_COMPILATION_ERROR' }
assert compilationIssue != null
assert compilationIssue.message == 'Error while compiling extension module myConnectorWithError'

// connector implementation not found, as the connector does not compile
def connectorImpl = report.connectorImplementations.find { it.definitionId == 'myConnectorWithError' }
assert connectorImpl == null
def implementationIssue = report.issues.find { it.type == 'INVALID_DESCRIPTOR_FILE' }
assert implementationIssue != null
assert implementationIssue.message == 'myConnector.impl declares an unknown \'implementationClassname\': com.company.example.MyConnector'

def definitionIssue = report.issues.find { it.type == 'UNKNOWN_DEFINITION_TYPE' }
assert definitionIssue != null
assert definitionIssue.message == 'myConnector.def declares a definition \'myConnectorWithError (0.1.0)\' but no matching implementation has been found. This definition will be ignored.'

