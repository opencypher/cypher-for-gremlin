# Gremlin Server Cypher Plugin

This is an [`OpProcessor`](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) implementation for Gremlin Server that translates Cypher queries to [`GraphTraversal`](https://tinkerpop.apache.org/docs/current/reference/#traversal).

## Prerequisites

- [Gremlin Server](https://tinkerpop.apache.org/) or a compatible graph database
- Ability to install Gremlin Server plugins

## Installation

1. Build the plugin JAR file:
   ```sh
   ./gradlew :plugin:server-plugin:shadowJar
   ```
1. Copy plugin shadow JAR file to Gremlin Server `lib/` directory:
   ```sh
   cp plugin/server-plugin/build/libs/server-plugin-*-all.jar /path/to/gremlin-server/lib/
   ```
1. [Register](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) the `org.opencypher.gremlin.server.op.cypher.CypherOpProcessor`.
1. Restart Gremlin Server.
1. If the plugin has been installed correctly, you should see the following line among the logs:
   ```
   [INFO] OpLoader - Adding the cypher OpProcessor.
   ```
