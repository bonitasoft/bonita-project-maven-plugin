![Build](https://github.com/bonitasoft/bonita-project-maven-plugin/workflows/workflow-build/badge.svg)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=bonitasoft_bonita-project-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=bonitasoft_bonita-project-maven-plugin)
[![GitHub release](https://img.shields.io/github/v/release/bonitasoft/bonita-project-maven-plugin?color=blue&label=Release)](https://github.com/bonitasoft/bonita-project-maven-plugin/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.bonitasoft.maven/bonita-project-maven-plugin.svg?label=Maven%20Central&color=orange)](https://search.maven.org/search?q=g:%22org.bonitasoft%22%20AND%20a:%22bonita-project-maven-plugin%22)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-yellow.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

# Bonita Project Maven Plugin

A Maven plug-in used by Bonita projects to: 
* Install custom dependencies from the project store (where external projects dependencies are stored) to the local Maven repository.
* Analyze Bonita dependencies and their content (Connector, Actor filters, Rest API Extensions...).

## Usage

### Install

It is possible to directly invoke the plug-in like this:  
```sh
~/my-bonita-project> mvn bonita-project:install
```

### Analyze

It is possible to directly invoke the plug-in like this:  
```sh
~/my-bonita-project> mvn bonita-project:analyze
```
The analysis report is written in the `target` folder


## Philosophy

The Bonita project store is a local folder where projects dependencies are stored. It follows the Maven repository folders layout to be able to match a dependency declared in the `pom.xml` of the project. The plugin execution tries to resolve declared dependencies in the `pom.xml` of the project and if it fails (the dependency cannot be resolved against any provided Maven repositories), it searches in the Project Store for a matching artifact and installs it in the local Maven repository.

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation of this project are released under the [GPLv2 License](LICENSE)
