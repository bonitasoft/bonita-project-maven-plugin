import java.io.*
import groovy.io.FileType

def outputProcesses = new File( basedir, 'app/target/processes');
def businessArchives = []
 outputProcesses.eachFileRecurse (FileType.FILES) {
     businessArchives << it
} 

assert businessArchives.name.containsAll(['Pool--1.0.bar',
                           'Pool1--1.0.bar']): 'Some expected business archives are missing from the output folder'
assert  new File( basedir, 'app/target/build-process-project-1.0.0-SNAPSHOT-Local.bconf').exists() : 'A Bonita Configuration file is missing in the output folder';