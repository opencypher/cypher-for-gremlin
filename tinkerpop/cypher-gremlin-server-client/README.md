# Cypher Client for Gremlin Server

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-client/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-client)

This is a convenience Gremlin Server client for Java that can send queries via a [Gremlin `Client`](https://tinkerpop.apache.org/docs/current/reference/#connecting-via-java) instance:

- to a remote Gremlin Server (with [Cypher plugin](../cypher-gremlin-server-plugin)),
- to any Gremlin Server or a compatible graph database, with client-side translation.

## Getting Started

To add a dependency using Maven:

```xml
<dependency>
    <groupId>org.opencypher.gremlin</groupId>
    <artifactId>cypher-gremlin-server-client</artifactId>
    <version>0.9.2</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'org.opencypher.gremlin:cypher-gremlin-server-client:0.9.2'
}
```

You can also [build the snapshot](../README.md#development) from source.

## Usage

To send Cypher queries to a Cypher-enabled Gremlin Server and get Cypher-style results:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClient.java#gremlinStyle) -->
```java
Cluster cluster = Cluster.open(configuration);
Client gremlinClient = cluster.connect();
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.plugin(gremlinClient);
String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
CypherResultSet resultSet = cypherGremlinClient.submit(cypher);
List<Map<String, Object>> results = resultSet.all();
```

Result sets can be consumed in various ways (all share the same iterator, so pick _one_):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClient.java#workingWithCypherGremlinClient) -->
```java
List<Map<String, Object>> list = cypherGremlinClient.submit(cypher).all(); // as a list
Stream<Map<String, Object>> stream = cypherGremlinClient.submit(cypher).stream(); // as a stream
Iterator<Map<String, Object>> iterator = cypherGremlinClient.submit(cypher).iterator(); // as an iterator
Iterable<Map<String, Object>> iterable = cypherGremlinClient.submit(cypher); // also an iterable
```

Queries can be submitted asynchronously:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClient.java#async) -->
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

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClient.java#translating) -->
```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(gremlinClient);
```

You can also execute Cypher directly against a [`GraphTraversalSource`](https://tinkerpop.apache.org/docs/current/reference/#the-graph-process):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClient.java#inMemory) -->
```java
TinkerGraph graph = TinkerFactory.createModern();
GraphTraversalSource traversal = graph.traversal();
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.inMemory(traversal);
String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
List<Map<String, Object>> results = cypherGremlinClient.submit(cypher).all();
```

Consult the Javadoc for more information.

## Neo4j driver-like API

If you want to use a Neo4j driver-like API, take a look at the [Cypher Gremlin Neo4j Driver](../cypher-gremlin-neo4j-driver).
