import java.io.*

File parametersFile = new File( basedir, '.bcd_configurations/parameters-local.yml');
assert parametersFile.exists() : 'parameters-local.yml is missing'