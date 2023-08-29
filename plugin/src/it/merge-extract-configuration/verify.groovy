import java.io.*

File parametersFile = new File( basedir, 'app/target/parameters-local.yml');
assert parametersFile.exists() : 'parameters-local.yml is missing'