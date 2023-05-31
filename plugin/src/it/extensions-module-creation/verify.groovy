import java.io.*

File extensionsPom = new File( basedir, 'extensions/pom.xml');
assert extensionsPom.exists() : 'extensions/pom.xml is missing'