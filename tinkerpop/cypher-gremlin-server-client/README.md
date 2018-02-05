# Cypher Client for Gremlin Server

This is a convenience Gremlin Server client for Java that can send Cypher queries to a remote Gremlin Server (with [Cypher-enabled](../cypher-gremlin-server-plugin)) via a [Gremlin `Client`](https://tinkerpop.apache.org/docs/current/reference/#connecting-via-java) instance.

## Usage

To send Cypher queries and get Cypher-style results:

```java
Cluster cluster = Cluster.open();
Client gremlinClient = cluster.connect();
CypherGremlinClient cypherGremlinClient = new CypherGremlinClient(gremlinClient);
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
List<Map<String, Object>> results = cypherGremlinClient.submit(cypher);
```

Queries can be submitted asynchronously:

```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
CompletableFuture<List<Map<String, Object>>> future = cypherGremlinClient.submitAsync(cypher);
future.thenAccept(results -> {
    // ...
});
```

Consult the Javadoc for more information.
