# Cypher Client for Gremlin Server

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-client/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-client)

This is a convenience Gremlin Server client for Java that can send queries via a [Gremlin `Client`](https://tinkerpop.apache.org/docs/current/reference/#connecting-via-java) instance:

- to a remote Gremlin Server with [Cypher plugin](../cypher-gremlin-server-plugin),
- with client-side translation: to a Gremlin Server-based database, Amazon Neptune, or Azure Cosmos DB.
## Getting Started

To add a dependency using Maven:

```xml
<dependency>
    <groupId>org.opencypher.gremlin</groupId>
    <artifactId>cypher-gremlin-server-client</artifactId>
    <version>1.0.2</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'org.opencypher.gremlin:cypher-gremlin-server-client:1.0.2'
}
```

You can also [build the snapshot](../README.md#development) from source.

## Usage

To send Cypher queries to a Cypher-enabled Gremlin Server and get Cypher-style results:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#gremlinStyle) -->
```java
Cluster cluster = Cluster.open(configuration);
Client gremlinClient = cluster.connect();
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.plugin(gremlinClient);
String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
CypherResultSet resultSet = cypherGremlinClient.submit(cypher);
List<Map<String, Object>> results = resultSet.all();
```

Result sets can be consumed in various ways (all share the same iterator, so pick _one_):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#workingWithCypherGremlinClient) -->
```java
List<Map<String, Object>> list = cypherGremlinClient.submit(cypher).all(); // as a list
Stream<Map<String, Object>> stream = cypherGremlinClient.submit(cypher).stream(); // as a stream
Iterator<Map<String, Object>> iterator = cypherGremlinClient.submit(cypher).iterator(); // as an iterator
Iterable<Map<String, Object>> iterable = cypherGremlinClient.submit(cypher); // also an iterable
```

Queries can be submitted asynchronously:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#async) -->
```java
String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
CompletableFuture<CypherResultSet> future = cypherGremlinClient.submitAsync(cypher);
future
    .thenApply(CypherResultSet::all)
    .thenAccept(resultSet -> {
        // ...
    });
```

If the target Gremlin Server does not have the [Cypher plugin](../cypher-gremlin-server-plugin) installed, translation can be done on the client's thread:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#translating) -->
```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(gremlinClient);
```

### Azure Cosmos DB

A translating client for Azure Cosmos DB can be configured like so:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#cosmosdb) -->
```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.retrieving(
    gremlinClient,
    TranslatorFlavor.cosmosDb()
);
```

### Amazon Neptune

A translating client for Amazon Neptune can be configured like so:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#neptune) -->
```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(
    gremlinClient,
    () -> Translator.builder()
        .gremlinGroovy()
        .inlineParameters()
        .enableMultipleLabels()
        .build(TranslatorFlavor.neptune())
);
```

### In-memory Client

You can also execute Cypher directly against a [`GraphTraversalSource`](https://tinkerpop.apache.org/docs/current/reference/#the-graph-process):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#inMemory) -->
```java
TinkerGraph graph = TinkerFactory.createModern();
GraphTraversalSource traversal = graph.traversal();
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.inMemory(traversal);
String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
List<Map<String, Object>> results = cypherGremlinClient.submit(cypher).all();
```

Consult the [Javadoc](https://opencypher.github.io/cypher-for-gremlin/api/1.0.2/java/org/opencypher/gremlin/client/package-summary.html) for more information.

## Neo4j driver-like API

If you want to use a Neo4j driver-like API, take a look at the [Cypher Gremlin Neo4j Driver](../cypher-gremlin-neo4j-driver).

## Cypher Traversal Source

With [CypherTraversalSource](https://opencypher.github.io/cypher-for-gremlin/api/1.0.2/java/org/opencypher/gremlin/client/CypherTraversalSource.html)
its possible to combine Cypher and Gremlin in single query. Traversal can start with `cypher` step that allows to run Cypher 
query (which will be translated to Gremlin) then continue traversal using other Gremlin steps. Note that `cypher` step returns list of maps, corresponding to rows and named columns.
To continue traversal with other Gremlin steps, use [select step](http://tinkerpop.apache.org/docs/current/reference/#select-step):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#cypherTraversalSource) -->
```java
CypherTraversalSource g = graph.traversal(CypherTraversalSource.class);

GraphTraversal<Map<String, Object>, String> query = g
    .cypher("MATCH (n) RETURN n")
    .select("n")
    .outE()
    .label()
    .dedup();
```

This approach can be used for remote databases using [withRemote](http://tinkerpop.apache.org/docs/current/reference/#connecting-gremlin-server).
Translation could be adapted for specific Gremlin implementation by passing [Flavor](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#flavors)
or enabling [Gremlin Extensions for Cypher](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#cypher-extensions):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#cypherTraversalWithRemote) -->
```java
CypherTraversalSource g = AnonymousTraversalSource
    .traversal(CypherTraversalSource.class)
    .withRemote(PATH_TO_REMOTE_PROPERTIES);

GraphTraversal<Map<String, Object>, String> traversal = g
    .cypher("MATCH (n) RETURN n", "cosmosdb")
    .select("n")
    .outE()
    .label()
    .dedup();
```

Note that Cypher query may return null values, represented by [string constant](https://opencypher.github.io/cypher-for-gremlin/api/1.0.2/java/constant-values.html#org.opencypher.gremlin.translation.Tokens.NULL).

## Gremlin function

Experimental `gremlin` Cypher function that allows including Gremlin steps in translated query. Note that currently
function is supported only for client-side translation, and should be enabled explicitly.

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#enableExperimentalGremlin) -->
```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(
    gremlinClient,
    () -> Translator.builder()
        .gremlinGroovy()
        .enableCypherExtensions()
        .enable(TranslatorFeature.EXPERIMENTAL_GREMLIN_FUNCTION)
        .build()
);

List<Map<String, Object>> results = cypherGremlinClient.submit(
    "MATCH (n:person {name: 'marko'}) " +
        "RETURN gremlin(\"select('n').outE().label()\") as r").all();
```


