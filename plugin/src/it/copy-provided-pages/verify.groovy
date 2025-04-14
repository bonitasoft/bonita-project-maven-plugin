import groovy.io.FileType

def outputFolder = new File(basedir, 'app/target/provided-pages');
assert outputFolder.exists(): "Output folder '${outputFolder}' does not exist"

def files = []
outputFolder.eachFileRecurse(FileType.FILES) {
    files << it.getName()
}

def bonitaVersion = '10.2.0'

def expectedFiles = [
        'page-user-case-details-' + bonitaVersion + '.zip',
        'page-user-case-list-' + bonitaVersion + '.zip',
        'page-user-process-list-' + bonitaVersion + '.zip',
        'page-user-task-list-' + bonitaVersion + '.zip'
]
assert expectedFiles.size() == files.size(): "Expected ${expectedFiles.size()} files but was ${files.size()}"
assert expectedFiles.containsAll(files): "Some expected files are missing"
