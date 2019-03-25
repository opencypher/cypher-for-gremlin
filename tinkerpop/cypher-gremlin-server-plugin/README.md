# Gremlin Server Cypher Plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-plugin/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-plugin)

Included in the plugin:

- [`OpProcessor`](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) implementation for Gremlin Server that translates Cypher queries to [`GraphTraversal`](https://tinkerpop.apache.org/docs/current/reference/#traversal)
- [Cypher extensions](../cypher-gremlin-extensions) for Gremlin in the form of custom functions and predicates

## Prerequisites

- [Gremlin Server](https://tinkerpop.apache.org/) based on TinkerPop 3.4.0+ or a compatible graph database
  - For 3.2.0+ compatibility use [0.9.13](https://github.com/opencypher/cypher-for-gremlin/releases/tag/v0.9.13)
- Ability to install Gremlin Server plugins

## Installation

The plugin and its dependencies can be automatically downloaded and installed into the Gremlin Server by using `bin/gremlin-server.sh install`. You can also manually "install" the plugin by copying jar file into the server classpath.

### Automated Installation

* For TinkerPop 3.3.x:
  - Run `bin/gremlin-server.sh` with `install` switch and supply the Maven coordinates of the plugin:

  ```sh
  bin/gremlin-server.sh install org.opencypher.gremlin cypher-gremlin-server-plugin 1.0.0
  Installing dependency org.opencypher.gremlin cypher-gremlin-server-plugin 1.0.0
  ...
  ```
* For TinkerPop 3.2.x (including JanusGraph):
  - Run `bin/gremlin-server.sh` with `-i` switch and supply the Maven coordinates of the plugin:
  ```sh
  bin/gremlin-server.sh -i org.opencypher.gremlin cypher-gremlin-server-plugin 0.9.13
  ...
  ```
  
### Manual Installation

Run the following commands from project root.

1. Download [the latest release](https://github.com/opencypher/cypher-for-gremlin/releases) or build the plugin JAR file:
   ```sh
   ./gradlew :tinkerpop:cypher-gremlin-server-plugin:shadowJar
   ```
1. Copy plugin JAR file to Gremlin Server `lib/` directory:
   ```sh
   cp tinkerpop/cypher-gremlin-server-plugin/build/libs/cypher-gremlin-server-plugin-*-all.jar /path/to/gremlin-server/lib/
   ```
1. [Register](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) the `org.opencypher.gremlin.server.op.cypher.CypherOpProcessor`.
1. Add `['org.opencypher.gremlin.process.traversal.CustomPredicates.*']` to [Gremlin Server configuration file](https://tinkerpop.apache.org/docs/current/reference/#_configuring_2) at `scriptEngines`/`gremlin-groovy`/`staticImports`.
1. Restart Gremlin Server.
1. If the plugin has been installed correctly, you should see the following line among the logs:
   ```
   [INFO] OpLoader - Adding the cypher OpProcessor.
   ```

## Troubleshooting

- Make sure that Gremlin Server or the database you are using is based on TinkerPop 3.4.0 or later.
