# Gremlin Console Cypher plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-console-plugin/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-console-plugin)

This plugin enables Cypher queries in [Gremlin Console](https://tinkerpop.apache.org/docs/current/tutorials/the-gremlin-console/).

The plugin can work with any Gremlin Server and perform translation to Gremlin in the Console. Alternatively, if a Gremlin Server has the [Cypher plugin](../cypher-gremlin-server-plugin) installed, the plugin can let the server handle the translation.

## Prerequisites

- [Gremlin Console](https://tinkerpop.apache.org/) 3.4.0+ (see [Troubleshooting](#troubleshooting))
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
    gremlin> :install org.opencypher.gremlin cypher-gremlin-console-plugin 1.0.0
    ==>Loaded: [org.opencypher.gremlin, cypher-gremlin-console-plugin, 1.0.0] - restart the console to use [opencypher.gremlin]
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

### Pre-configured Distribution

You can also download a pre-configured Gremlin Console distribution from the [releases section](https://github.com/opencypher/cypher-for-gremlin/releases). This distribution has the Cypher plugin pre-installed.

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

     Where [flavor](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#flavors) is one of:
     `gremlin` (default), `gremlin33x` (for Gremlin Servers with TinkerPop <3.4.0), `neptune`, or `cosmosdb`.
     This enables Cypher to Gremlin translation in the Console plugin when submitting queries.

1. Submit Cypher queries using the `:>` command:
   ```
   gremlin> :> MATCH (p:person) RETURN p.name
   ==>marko
   ==>vadas
   ==>josh
   ==>peter
   ```
   
1. See translation using `:> EXPLAIN` command:
  ```
  gremlin> :> EXPLAIN MATCH (p:person) RETURN p
  ==>[translation:g.V().hasLabel('person').project('p').by(__.valueMap().with('~tinkerpop.valueMap.tokens')),options:[EXPLAIN]]  
  ```

### Amazon Neptune

To connect to Amazon Neptune from Gremlin Console:

```
gremlin> :plugin use opencypher.gremlin
==>opencypher.gremlin activated
gremlin> :remote connect opencypher.gremlin conf/remote-objects.yaml translate neptune
==>Configured <instance>.<region>.compute.amazonaws.com/<ip>:<port>
 ```

### Azure Cosmos DB

To connect to Azure Cosmos DB from Gremlin Console:

```
gremlin> :plugin use opencypher.gremlin
==>opencypher.gremlin activated
gremlin> :remote connect opencypher.gremlin conf/remote-objects.yaml translate cosmosdb
==>Configured <instance>.graphs.azure.com/<ip>:<port>
 ```
 
### Combining Cypher and Gremlin

With [CypherTraversalSource](https://opencypher.github.io/cypher-for-gremlin/api/1.0.0/java/org/opencypher/gremlin/client/CypherTraversalSource.html)
its possible to combine Cypher and Gremlin in single query. Traversal can start with `cypher` step that allows to run Cypher 
query (which will be translated to Gremlin) then continue traversal using other Gremlin steps. Note that `cypher` step returns list of maps, corresponding to rows and named columns.
To continue traversal with other Gremlin steps, use [select step](http://tinkerpop.apache.org/docs/current/reference/#select-step):

```
gremlin> :plugin use opencypher.gremlin
==>opencypher.gremlin activated
gremlin> graph = TinkerFactory.createModern();
==>tinkergraph[vertices:6 edges:6]
gremlin> g = graph.traversal(CypherTraversalSource.class)
==>cyphertraversalsource[tinkergraph[vertices:6 edges:6], standard]
gremlin> g.cypher('MATCH (p:person) RETURN p').select("p").outE().label().dedup()
==>created
==>knows
```

This approach can be used for remote databases using [withRemote](http://tinkerpop.apache.org/docs/current/reference/#connecting-gremlin-server).
Translation could be adapted for specific Gremlin implementation by passing [Flavor](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#flavors)
or enabling [Cypher for Gremlin extensions](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#cypher-extensions):

```
gremlin> :plugin use opencypher.gremlin
==>opencypher.gremlin activated
gremlin> g = EmptyGraph.instance().traversal(CypherTraversalSource.class).withRemote('conf/remote-graph.properties')"
==>cyphertraversalsource[emptygraph[empty], standard]
gremlin> g.cypher('MATCH (p:person) RETURN p.name AS name', 'cosmosdb')
==>[name:marko]
==>[name:vadas]
==>[name:josh]
==>[name:peter]
```

Note that Cypher query may return null values, represented by [string constant](https://opencypher.github.io/cypher-for-gremlin/api/1.0.0/java/constant-values.html#org.opencypher.gremlin.translation.Tokens.NULL).   

## Troubleshooting

* Executing a Cypher query causes an error: `java.lang.ClassCastException: java.lang.String cannot be cast to java.util.Map`.
  - Make sure that Gremlin Console has `org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0` serializer with `serializeResultToString` disabled.
* Executing a Cypher query causes an error: `java.lang.NoClassDefFoundError:`.
  - Upgrade Gremlin Console to TinkerPop 3.4.0+, or use earlier (TinkerPop 3.3.x) version of [Cypher for Gremlin 0.9.13](https://github.com/opencypher/cypher-for-gremlin/releases/tag/v0.9.13)   
* After installation, plugin does not appear in `:plugin list`
  - Cypher for Gremlin requires Gremlin Console 3.3.0+ (See #254)
