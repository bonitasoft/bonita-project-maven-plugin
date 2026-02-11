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


## How to release

This project uses the [gitflow-maven-plugin](https://github.com/aleksandr-m/gitflow-maven-plugin) for release
management. Releases are created using the GitHub Actions workflow.

### Branch Strategy

- **develop**: Main development branch for future releases
- **support/A.B.x**: Maintenance branches for older versions (e.g., support/1.0.x, support/2.0.x, support/2.1.x)
- **master**: Not used (removed, use support branches for maintenance)

### Creating a Release

Releases are created via the GitHub Actions
workflow [Release](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/release.yml)

#### Workflow Parameters

| Parameter                   | Description                                                                  | Default     | Example                         |
|-----------------------------|------------------------------------------------------------------------------|-------------|---------------------------------|
| **version**                 | Version to release (leave empty to use current pom.xml version)              | empty       | `2.1.3`                         |
| **nextDevelopmentVersion**  | Next development version (leave empty to use versionDigitToIncrement policy) | empty       | `2.1.4-SNAPSHOT`                |
| **versionDigitToIncrement** | Version digit to increment in next development version                       | `2` (patch) | `0`=major, `1`=minor, `2`=patch |

#### Release Types

**Patch Release (X.Y.Z)** - Bug fixes and minor updates

- Set `versionDigitToIncrement`: **2** (default)
- Example: `2.1.2 → 2.1.3-SNAPSHOT`
- Use for: Support branches, hotfixes

**Minor Release (X.Y.0)** - New features, backward compatible

- Set `versionDigitToIncrement`: **1**
- Example: `2.1.2 → 2.2.0-SNAPSHOT`
- Use for: Regular releases from develop

**Major Release (X.0.0)** - Breaking changes

- Set `versionDigitToIncrement`: **0**
- Example: `2.1.2 → 3.0.0-SNAPSHOT`
- Use for: Major version bumps

### Release Process

When you run the workflow from any branch (develop or support/*), it will:

1. Update version to release version (removes -SNAPSHOT)
2. Commit: "chore(release): Update versions for release"
3. Create git tag (e.g., `2.1.3`)
4. Update version to next development version
5. Commit: "chore(release): Update for next development version"
6. Push commits and tags to GitHub

**Note**: The workflow does NOT create a release branch - it performs the release directly on the branch you selected.

### Publishing Artifacts to Maven Central

Publication is done via the [Publish](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/publish.yml) workflow which will build and deploy a given tag to Maven Central.

### Publishing the Maven Site

Deploy the latest site version using the [Publish Maven Site](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/publish-site.yml) workflow.

### Cascade Merging for Support Branches

When releasing from a **support branch**, you should manually cascade merge the changes up to newer branches and
develop.

**Example**: After doing a release from `support/1.0.x`, merge `support/1.0.x` into `support/2.0.x`, then merge
`support/2.0.x` into `support/2.1.x`, and so on into `develop`.

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation of this project are released under the [GPLv2 License](LICENSE)
