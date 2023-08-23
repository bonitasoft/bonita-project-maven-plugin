import java.io.*

File bdmModel = new File( basedir, 'bdm/model/target/procurement-example-bdm-model-1.0.0-SNAPSHOT.jar');
assert bdmModel.exists() : 'bdm-model-1.0.0-SNAPSHOT.jar is missing'

File bdmModelDescriptor = new File( basedir, 'bdm/model/target/procurement-example-bdm-model-1.0.0-SNAPSHOT-descriptor.zip');
assert bdmModelDescriptor.exists() : 'bdm-model-1.0.0-SNAPSHOT-descriptor.zip is missing'

File bdmModelSources = new File( basedir, 'bdm/model/target/procurement-example-bdm-model-1.0.0-SNAPSHOT-sources.jar');
assert bdmModelSources.exists() : 'bdm-model-1.0.0-SNAPSHOT-sources.jar is missing'

File bdmClientDao = new File( basedir, 'bdm/dao-client/target/procurement-example-bdm-dao-client-1.0.0-SNAPSHOT.jar');
assert bdmClientDao.exists() : 'procurement-example-bdm-dao-client-1.0.0-SNAPSHOT.jar'

File bdmClientDaoSources = new File( basedir, 'bdm/dao-client/target/procurement-example-bdm-dao-client-1.0.0-SNAPSHOT-sources.jar');
assert bdmClientDaoSources.exists() : 'procurement-example-bdm-dao-client-1.0.0-SNAPSHOT-sources.jar'

File appPom = new File( basedir, 'app/pom.xml');
assert appPom.text.contains("<artifactId>procurement-example-bdm-model</artifactId>")