[![Build](https://github.com/bonitasoft/bonita-project-maven-plugin/workflows/workflow-build/badge.svg)](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/workflow-build.yml)
[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=bonitasoft_bonita-project-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=bonitasoft_bonita-project-maven-plugin)
[![GitHub release](https://img.shields.io/github/v/release/bonitasoft/bonita-project-maven-plugin?color=blue&label=Release)](https://github.com/bonitasoft/bonita-project-maven-plugin/releases)
[![Maven Central](https://img.shields.io/maven-central/v/org.bonitasoft.maven/bonita-project-maven-plugin.svg?label=Maven%20Central&color=orange)](https://search.maven.org/search?q=g:%22org.bonitasoft%22%20AND%20a:%22bonita-project-maven-plugin%22)
[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-yellow.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)

# Bonita Project Maven Plugin

A Maven plug-in used by Bonita projects to: 
* Install custom dependencies from the project store (where external projects dependencies are stored) to the local Maven repository.
* Analyze Bonita dependencies and their content (Connector, Actor filters, Rest API Extensions...).
* Generate Business Object Model artifacts from the BDM descriptor file
* Generate Extensions submodule
* Validate Bonita model artifacts
* Build Business Archive from a `.proc` files
* Build UID pages
* Extract processes configuration from `.proc` files.
* Merge parameters into the Bonita configuration file 

## Plugin Documentation Site

https://bonitasoft.github.io/bonita-project-maven-plugin/


## Branching strategy

This repository follows the [GitFlow branching strategy](https://gitversion.net/docs/learn/branching-strategies/gitflow/examples).

## Release

To release a new version, maintainers may use the Release and Publication GitHub actions.

* Release action will invoke the `gitflow-maven-plugin` to perform all required merges, version updates and tag creation.
* Publication action will build and deploy a given tag to Maven Central.
* A Github release should be created and associated to the tag.
* Deploy the latest site version using the [Publish Maven Site action](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/publish-site.yml)

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation of this project are released under the [GPLv2 License](LICENSE)
