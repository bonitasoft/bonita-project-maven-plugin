# Bonita Project Store Maven Plugin

A Maven plug-in used by Bonita project to synchronize the project store, where external project dependencies can be stored, and the local Maven repository.

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

The Bonita project store is local folder where project dependencies are stored. It follows the Maven repository folder layout to be able to match a dependency declared in the `pom.xml` of the project. The plugin execution tries to resolved decalred dependencies in the `pom.xml` of the project and when it fails (the dependency cannot be resolved against any provided Maven repository), it searches in the project store for a matching artifact and installs it in the local Maven repository.

## Contributing

We would love you to contribute, pull requests are welcome! Please see the [CONTRIBUTING.md](CONTRIBUTING.md) for more information.

## License

The sources and documentation in this project are released under the [GPLv2 License](LICENSE)