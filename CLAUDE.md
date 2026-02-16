# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Bonita Project Maven Plugin — a multi-module Maven plugin for the Bonita Platform that handles dependency analysis, BDM code generation, artifact validation, Business Archive building, UID page building, and configuration management.

**Modules:** `plugin/` (main plugin), `report-model/` (dependency report data models), `jacoco-coverage-report/` (coverage aggregation).

This plugin is used by [bonita-project](https://github.com/bonitasoft/bonita-project).

## Build Commands

```bash
./mvnw clean install                    # Full build with tests
./mvnw clean verify                     # Build + unit & integration tests
./mvnw test                             # Unit tests only
./mvnw test -pl plugin                  # Unit tests for plugin module only
./mvnw test -Dtest=ClassName            # Single test class
./mvnw test -Dtest=ClassName#method     # Single test method
./mvnw spotless:check                   # Check code formatting
./mvnw spotless:apply                   # Apply code formatting
```

Integration tests use Maven Invoker Plugin with Groovy verification scripts located in `plugin/src/it/`.

```bash
./mvnw invoker:install                                   # Install plugin locally for IT
./mvnw invoker:run                                       # Run all integration tests
./mvnw invoker:run -Dinvoker.test=bdm-module-generation  # Run a specific IT (folder name under plugin/src/it/)
```

## Tech Stack

- **Java 17**, Maven 3.6+ (wrapper uses 3.9.9)
- **Testing:** JUnit Jupiter, Mockito, AssertJ
- **Libraries:** Jackson (JSON), Lombok (annotations), Commons IO
- **Code formatting:** Spotless plugin with Eclipse formatter (`formatter.xml`), import order (`eclipse.importorder`), and license header (`header.txt`)

## Architecture

The plugin provides these Maven goals, each implemented as a Mojo in `plugin/src/main/java/org/bonitasoft/plugin/`:

| Goal | Mojo | Package |
|------|------|---------|
| `analyze` | `AnalyzeBonitaDependencyMojo` (aggregator) | `analyze/` |
| `install` | `InstallProjectStoreMojo` | `install/` |
| `validate` | `ValidateMojo` (phase: VALIDATE) | `validation/` |
| `generate-bdm-model` | `GenerateBdmModelSourceMojo` | `bdm/codegen/` |
| `generate-bdm-dao-client` | `GenerateBdmDaoClientSourceMojo` | `bdm/codegen/` |
| `create-bdm-module` | `CreateBdmModuleMojo` | `bdm/module/` |
| `create-extensions-module` | `CreateExtensionsModuleMojo` | `extension/` |
| `business-archive` | `BuildBarMojo` | `build/bar/` |
| `uid-page` | `BuildUidPageMojo` | `build/page/` |
| `copy-provided-pages` | `CopyProvidedPagesMojo` | `build/` |
| `extract-configuration` | `ExtractConfigurationArchiveMojo` | `build/` |
| `merge-configuration` | `MergeConfigurationArchiveMojo` | `build/` |

**Key patterns:**
- **Factory pattern** for artifact analyzers (`DefaultArtifactAnalyzerFactory`) and UID builders (`UidArtifactBuilderFactory`)
- **Strategy/Handler chain** for analyzing different artifact types (`analyze/handler/` — `ConnectorAnalyzer`, `CustomPageAnalyzer`, `ApplicationDescriptorAnalyzer`)
- **Template method** in `AbstractUidValidationTask` for UID validation variants (pages, fragments, widgets)
- **Content readers** (`analyze/content/`) abstract over JAR, ZIP, and project artifact formats
- Report model classes in `report-model/` use Lombok `@Data`
- Maven dependency injection via `@Inject`

## Commit Message Format

Follow conventional commits: `type(category): description`

Types: `breaking`, `build`, `ci`, `chore`, `docs`, `feat`, `fix`, `other`, `perf`, `refactor`, `revert`, `style`, `test`

## Branch Strategy

- **develop**: Main development branch for future releases
- **support/A.B.x**: Maintenance branches for older versions (e.g., support/1.0.x, support/2.0.x, support/2.1.x)
- **master**: Not used (removed, use support branches for maintenance)

No release branches are created — `gitflow:release` performs the release directly on the current branch.

## Release & Publish

- **Release:** via [Release workflow](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/release.yml) — uses [`gitflow-maven-plugin`](https://github.com/aleksandr-m/gitflow-maven-plugin) (`gitflow:release` goal) with `skipReleaseMergeProdBranch=true` (no merge into master). Workflow parameters: `version`, `nextDevelopmentVersion`, `versionDigitToIncrement` (0=major, 1=minor, 2=patch, default 2).
- **Publish artifacts:** via [Publish workflow](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/publish.yml) — builds and deploys a given tag to Maven Central.
- **Publish site:** via [Publish Maven Site workflow](https://github.com/bonitasoft/bonita-project-maven-plugin/actions/workflows/publish-site.yml).
- After releasing from a support branch, manually cascade merge up to newer support branches and into develop.

## Coupled Versions

Some Java constants must stay in sync with POM properties. When Dependabot (or a manual change) updates a POM version, update the corresponding constant too:

| POM property | Java constant | File |
|---|---|---|
| `maven-install-plugin.version` | `DEFAULT_INSTALL_PLUGIN_VERSION` | `plugin/src/main/java/org/bonitasoft/plugin/install/InstallProjectStoreMojo.java` |

## CI/CD

- GitHub Actions on push to `develop`, `support/*` and PRs
- SonarCloud quality analysis integrated into builds
