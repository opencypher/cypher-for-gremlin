# Cypher Client for Gremlin Server

This is a convenience Gremlin Server client for Java that can send queries via a [Gremlin `Client`](https://tinkerpop.apache.org/docs/current/reference/#connecting-via-java) instance:

- to a remote Gremlin Server (with [Cypher plugin](../cypher-gremlin-server-plugin)),
- to any Gremlin Server or a compatible graph database, with client-side translation.

## Usage

To send Cypher queries to a Cypher-enabled Gremlin Server and get Cypher-style results:

```java
Cluster cluster = Cluster.open();
Client gremlinClient = cluster.connect();
CypherGremlinClient cypherGremlinClient = CypherGremlinClientFactory.plugin(gremlinClient);
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

If the target Gremlin Server does not have the [Cypher plugin](../cypher-gremlin-server-plugin) installed, translation can be done on the client's thread:

```java
CypherGremlinClient cypherGremlinClient = CypherGremlinClientFactory.translating(gremlinClient);
```

Consult the Javadoc for more information.
