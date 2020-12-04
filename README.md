# Bonita Project Store Maven Plugin

A Maven plug-in used by Bonita projects to synchronize the Project Store (where external projects dependencies can be stored) and the local Maven repository.

## Usage

It is possible to directly invoke the plug-in like this:  
```sh
~/my-bonita-project> mvn org.bonitasoft:bonita-project-store-maven-plugin:install
```

Or use the `validate` goal:
```sh
~/my-bonita-project> mvn validate
```

## Philosophy

The Bonita project store is a local folder where projects dependencies are stored. It follows the Maven repository folders layout to be able to match a dependency declared in the `pom.xml` of the project. The plugin execution tries to resolve declared dependencies in the `pom.xml` of the project and if it fails (the dependency cannot be resolved against any provided Maven repositories), it searches in the Project Store for a matching artifact and installs it in the local Maven repository.

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation of this project are released under the [GPLv2 License](LICENSE)
