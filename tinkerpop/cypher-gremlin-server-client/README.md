# Cypher Client for Gremlin Server

This is a convenience Gremlin Server client for Java that can send queries via a [Gremlin `Client`](https://tinkerpop.apache.org/docs/current/reference/#connecting-via-java) instance:

- to a remote Gremlin Server (with [Cypher plugin](../cypher-gremlin-server-plugin)),
- to any Gremlin Server or a compatible graph database, with client-side translation.

## Usage

To send Cypher queries to a Cypher-enabled Gremlin Server and get Cypher-style results:

```java
Cluster cluster = Cluster.open();
Client gremlinClient = cluster.connect();
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.plugin(gremlinClient);
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
CypherResultSet resultSet = cypherGremlinClient.submit(cypher);
List<Map<String, Object>> results = resultSet.all();
```

Result sets can be consumed in various ways (all share the same iterator, so pick _one_):

```java
CypherResultSet resultSet = cypherGremlinClient.submit(cypher);
List<Map<String, Object>> list = resultSet.all(); // as a list
Stream<Map<String, Object>> stream = resultSet.stream(); // as a stream
Iterator<Map<String, Object>> iterator = resultSet.iterator(); // as an iterator
Iterable<Map<String, Object>> iterable = resultSet; // also an iterable
```

Queries can be submitted asynchronously:

```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
CompletableFuture<CypherResultSet> future = cypherGremlinClient.submitAsync(cypher);
future
    .thenApply(CypherResultSet::all)
    .thenAccept(resultSet -> {
        // ...
    });
```

If the target Gremlin Server does not have the [Cypher plugin](../cypher-gremlin-server-plugin) installed, translation can be done on the client's thread:

```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(gremlinClient);
```

You can also execute Cypher directly against a [`GraphTraversalSource`](https://tinkerpop.apache.org/docs/current/reference/#the-graph-process):

```java
TinkerGraph graph = TinkerFactory.createModern();
GraphTraversalSource traversal = graph.traversal();
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.inMemory(traversal);
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
List<Map<String, Object>> results = cypherGremlinClient.submit(cypher).all();
```

Consult the Javadoc for more information.
