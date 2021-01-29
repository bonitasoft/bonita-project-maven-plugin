![Build](https://github.com/bonitasoft/bonita-project-maven-plugin/workflows/workflow-build/badge.svg)

# Bonita Project Maven Plugin

A Maven plug-in used by Bonita projects to: 
* Install custom dependencies from the project store (where external projects dependencies are stored) to the local Maven repository.
* Analyze Bonita dependencies and their content (Connector, Actor filters, Rest API Extensions...)
* Lookup jar dependencies (find the maven artifacts for existing dependencies found in the `lib` folder)

## Usage

### Install

It is possible to directly invoke the plug-in like this:  
```sh
~/my-bonita-project> mvn org.bonitasoft:bonita-project-maven-plugin:install
```

Or use the `validate` goal:
```sh
~/my-bonita-project> mvn validate
```

### Analyze

It is possible to directly invoke the plug-in like this:  
```sh
~/my-bonita-project> mvn org.bonitasoft:bonita-project-maven-plugin:analyze
```
The  analysis report is written in the `target` folder


## Philosophy

The Bonita project store is a local folder where projects dependencies are stored. It follows the Maven repository folders layout to be able to match a dependency declared in the `pom.xml` of the project. The plugin execution tries to resolve declared dependencies in the `pom.xml` of the project and if it fails (the dependency cannot be resolved against any provided Maven repositories), it searches in the Project Store for a matching artifact and installs it in the local Maven repository.

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation of this project are released under the [GPLv2 License](LICENSE)
