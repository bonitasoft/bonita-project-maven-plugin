import java.io.*
import groovy.io.FileType

def outputPages = new File( basedir, 'target/pages');
def pages = []
 outputPages.eachFileRecurse (FileType.FILES) {
     pages << it
} 

assert pages.name.containsAll(['layout_BonitaLayoutV9.zip',
                            'page_AdminPage.zip', 
                            'page_POReleaseFollowUp.zip',
                            'page_SerenityPOTaskList.zip',
                            'page_SerenityTaskList.zip']): 'Some expected page are missing from the output folder'
                        
                        
def outputExlusionPages = new File( basedir, 'target/exclusion/pages');
pages = []
outputExlusionPages.eachFileRecurse (FileType.FILES) {
     pages << it
}

assert !new File(outputExlusionPages, 'page_AdminPage.zip' ).exists() : 'page_AdminPage.zip should have been excluded'
assert pages.name.containsAll(['layout_BonitaLayoutV9.zip',
                            'page_POReleaseFollowUp.zip',
                            'page_SerenityPOTaskList.zip',
                            'page_SerenityTaskList.zip']): 'Some expcted page are missing from the output folder'
                        
def outputInclusionPages = new File( basedir, 'target/inclusion/pages');
pages = []
outputInclusionPages.eachFileRecurse (FileType.FILES) {
     pages << it
}

assert pages.size() == 1 : 'Some pages should not have been built'
assert new File(outputInclusionPages, 'page_AdminPage.zip' ).exists() : 'page_AdminPage.zip should have been included'
