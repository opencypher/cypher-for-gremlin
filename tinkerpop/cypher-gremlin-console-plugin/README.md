# Gremlin Console Cypher plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-console-plugin/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-console-plugin)

This plugin enables Cypher queries in [Gremlin Console](https://tinkerpop.apache.org/docs/current/tutorials/the-gremlin-console/).

The plugin can work with any Gremlin Server and perform translation to Gremlin in the Console. Alternatively, if a Gremlin Server has the [Cypher plugin](../cypher-gremlin-server-plugin) installed, the plugin can let the server handle the translation.

## Prerequisites

- [Gremlin Console](https://tinkerpop.apache.org/)
- A running [Gremlin Server](https://tinkerpop.apache.org/) or a compatible graph database

## Installation

The plugin and its dependencies can be automatically downloaded and installed into the Gremlin Console by using console `:install` command. You can also manually "install" the plugin by copying jar file into the Gremlin Console classpath.

### Automated Installation

1. Launch Gremlin Console:
    ```sh
    bin/gremlin.sh
    ```

1. Install Gremlin Console Cypher plugin:
    ```
    gremlin> :install org.opencypher.gremlin cypher-gremlin-console-plugin 0.9.1
    ==>Loaded: [org.opencypher.gremlin, cypher-gremlin-console-plugin, 0.9.1] - restart the console to use [opencypher.gremlin]
    ```

1. Restart Gremlin Console
    ```
    gremlin> :q
    bin/gremlin.sh
    ```

### Manual Installation

Run the following commands from project root.

1. Download [the latest release](https://github.com/opencypher/cypher-for-gremlin/releases) or build the plugin JAR file:
    ```sh
    ./gradlew :tinkerpop:cypher-gremlin-console-plugin:shadowJar
    ```
1. Copy plugin JAR file to Gremlin Console `lib/` directory:
    ```sh
    cp tinkerpop/cypher-gremlin-console-plugin/build/libs/cypher-gremlin-console-plugin-*-all.jar /path/to/gremlin-console/lib/
    ```
1. Export `JAVA_OPTIONS`:
    ```sh
    export JAVA_OPTIONS="-Dplugins=v3d3"
    ```
1. Launch Gremlin Console:
    ```sh
    bin/gremlin.sh
    ```
1. If the plugin has been installed correctly, you should see it among the plugin list:
    ```
    gremlin> :plugin list
    ==>opencypher.gremlin
    ```

## Usage

1. Activate `opencypher.gremlin` plugin:
    ```
    gremlin> :plugin use opencypher.gremlin
    ==>opencypher.gremlin activated 
    ```
1. Connect to Gremlin Server.
   * With server-side translation ([Cypher plugin](../cypher-gremlin-server-plugin) required):
    ```
    gremlin> :remote connect opencypher.gremlin conf/remote-objects.yaml
    ==>Configured localhost/127.0.0.1:8182
    ```
   * With translation in Console:

     Append `translate [flavor]` option to the `connect` command:

     ```
     gremlin> :remote connect opencypher.gremlin conf/remote-objects.yaml translate gremlin
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

## Troubleshooting

* Executing a Cypher query causes an error: `java.lang.ClassCastException: java.lang.String cannot be cast to java.util.Map`.
  - Make sure that Gremlin Console has `org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0` serializer with `serializeResultToString` disabled.
