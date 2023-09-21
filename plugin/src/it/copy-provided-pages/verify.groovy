import groovy.io.FileType

def outputFolder = new File(basedir, 'app/target/provided-pages');
assert outputFolder.exists(): "Output folder '${outputFolder}' does not exist"

def files = []
outputFolder.eachFileRecurse(FileType.FILES) {
    files << it.getName()
}

def expectedFiles = [
        'page-user-case-details-8.0.0.zip',
        'page-user-case-list-8.0.0.zip',
        'page-user-process-list-8.0.0.zip',
        'page-user-task-list-8.0.0.zip'
]
assert expectedFiles.size() == files.size(): "Expected ${expectedFiles.size()} files but was ${files.size()}"
assert expectedFiles.containsAll(files): "Some expected files are missing"
