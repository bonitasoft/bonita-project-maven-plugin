import java.io.*

File parametersFile = new File( basedir, 'app/target/parameters-Local.yml');
assert parametersFile.exists() : 'parameters-Local.yml is missing'