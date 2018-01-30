# Gremlin Console Cypher plugin

This plugin enables Cypher queries in [Gremlin Console](https://tinkerpop.apache.org/docs/current/tutorials/the-gremlin-console/).

The plugin can work with any Gremlin Server and perform translation to Gremlin in the Console. Alternatively, if a Gremlin Server has the [Cypher plugin](../server-plugin) installed, the plugin can let the server handle the translation.

## Prerequisites

- Gremlin Console
- A running Gremlin Server or a compatible graph database

### Installation

1. Build the plugin JAR file:
    ```sh
    ./gradlew :plugin:console-plugin:shadowJar
    ```
1. Copy plugin shadow JAR file to Gremlin Console `lib/` directory:
    ```sh
    cp plugin/console-plugin/build/libs/console-plugin-*-all.jar /path/to/gremlin-console/lib/
    ```
1. Export `JAVA_OPTIONS`:
    ```sh
    export JAVA_OPTIONS="-Dplugins=v3d3"
    ```
1. Launch Gremlin Console.
1. If the plugin has been installed correctly, you should see it among the plugin list:
    ```
    gremlin> :plugin list
    ==>opencypher.gremlin
    ```

### Usage

1. Activate `opencypher.gremlin` plugin:
    ```
    gremlin> :plugin use opencypher.gremlin
    ==>opencypher.gremlin activated 
    ```
1. Connect to Gremlin Server.
   * With server-side translation ([Cypher plugin](../server-plugin) required):
    ```
    gremlin> :remote connect opencypher.gremlin conf/remote.yaml
    ==>Configured localhost/127.0.0.1:8182
    ```
   * With translation in Console:

     Append `translate [flavor]` option to the `connect` command:

     ```
     gremlin> :remote connect opencypher.gremlin conf/remote.yaml translate gremlin
     ==>Configured localhost/127.0.0.1:8182
     ```

     This enables Cypher to Gremlin translation in the Console plugin when submitting queries.
     Supported values of optional `flavor` parameter are `gremlin` (default) and `cosmosdb`.

1. Submit Cypher queries using the `:>` command:
   ```
   gremlin> :> MATCH (p:person) RETURN p.name
   ==>marko
   ==>vadas
   ==>josh
   ==>peter
   ```
