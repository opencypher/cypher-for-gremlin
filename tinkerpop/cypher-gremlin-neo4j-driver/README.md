# Cypher Gremlin Neo4j Driver for Java

This module provides Cypher for Gremlin implementation of Neo4j driver interfaces.

It could be used as a drop in replacement to run Cypher queries via [Neo4j Java API](https://neo4j.com/developer/java/) on [Gremlin Server](https://tinkerpop.apache.org/) or a compatible graph database.

## Differences from Neo4j Driver

* Transaction functionality is not supported yet.
* `SummaryCounters`, `StatementType` and `TypeSystem` are not supported.

## Getting Started

Cypher Gremlin Neo4j Driver module releases are not yet available on Maven Central, but will be soon! For now, you can [build the snapshot](../../README.md#development) from source. The built translation artifact will be in `tinkerpop/cypher-gremlin-neo4j-driver/build/libs`.

You can then install it manually.

## Usage

Use `org.opencypher.GremlinDatabase` to create Gremlin Server-enabled driver instances (like `org.neo4j.driver.v1.GraphDatabase`):

```java
Driver driver = GremlinDatabase.driver("//localhost:8182");
```

It is also possible to create a driver from a `org.apache.tinkerpop.gremlin.driver.Cluster` instance:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriver.java#createDriver) -->
```java
Cluster cluster1 = Cluster.build()
    .enableSsl(true)
    .addContactPoints("192.168.0.145")
    //...
    .create();
Driver driver1 = GremlinDatabase.driver(cluster1);

// Or:
Cluster cluster2 = Cluster.open(pathToGremlinConfiguration);

Driver driver2 = GremlinDatabase.driver(cluster2);
```

By default Cypher queries will be sent without translation, expecting [Cypher plugin](../cypher-gremlin-server-plugin) to be installed on the server. If the target Gremlin Server does not have the plugin installed, translation can be done on the client's thread:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriver.java#createConfiguration) -->
```java
Config config = Config.build()
    .withTranslation()
    .toConfig();

String uri = "//localhost:" + port;
Driver driver = GremlinDatabase.driver(uri, config);
```

Otherwise, the API is the same:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriver.java#useDriver) -->
```java
try (Session session = driver.session()) {
    StatementResult result = session.run("CREATE (a:Greeting) " +
            "SET a.message = $message " +
            "RETURN a.message",
        parameters("message", "Hello"));

    String message = result.single().get(0).asString();

    assertThat(message).isEqualTo("Hello");
}
```
