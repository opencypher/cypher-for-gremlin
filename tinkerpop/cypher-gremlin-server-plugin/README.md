# Gremlin Server Cypher Plugin

This is an [`OpProcessor`](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) implementation for Gremlin Server that translates Cypher queries to [`GraphTraversal`](https://tinkerpop.apache.org/docs/current/reference/#traversal).

## Prerequisites

- [Gremlin Server](https://tinkerpop.apache.org/) based on TinkerPop 3.3.0+ or a compatible graph database  
- Ability to install Gremlin Server plugins

## Installation

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

- Make sure that Gremlin Server or the database you are using is based on TinkerPop 3.3.0 or later.
