# Cypher Gremlin Neo4j Driver for Java

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-neo4j-driver/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-neo4j-driver)

This module provides Cypher for Gremlin implementation of Neo4j driver interfaces.

It could be used as a drop in replacement to run Cypher queries via [Neo4j Java API](https://neo4j.com/developer/java/) on [Gremlin Server](https://tinkerpop.apache.org/) or a compatible graph database.

## Differences from Neo4j Driver

* Transaction functionality is not supported yet.
* `SummaryCounters`, `StatementType` and `TypeSystem` are not supported.

## Getting Started

To add a dependency using Maven:

```xml
<dependency>
    <groupId>org.opencypher.gremlin</groupId>
    <artifactId>cypher-gremlin-neo4j-driver</artifactId>
    <version>0.9.9</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'org.opencypher.gremlin:cypher-gremlin-neo4j-driver:0.9.9'
}
```

You can also [build the snapshot](../README.md#development) from source.

## Usage

Use `org.opencypher.gremlin.neo4j.driver.GremlinDatabase` to create Gremlin Server-enabled driver instances (like `org.neo4j.driver.v1.GraphDatabase`):

```java
Driver driver = GremlinDatabase.driver("//localhost:8182");
```

It is also possible to create a driver from a `org.apache.tinkerpop.gremlin.driver.Cluster` instance:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriverSnippets.java#createDriver) -->
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

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriverSnippets.java#createConfiguration) -->
```java
Config config = Config.build()
    .withTranslation()
    .toConfig();

String uri = "//localhost:" + port;
Driver driver = GremlinDatabase.driver(uri, config);
```

You can also execute Cypher directly against a [`GraphTraversalSource`](https://tinkerpop.apache.org/docs/current/reference/#the-graph-process):

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriverSnippets.java#inMemory) -->
```java
TinkerGraph graph = TinkerFactory.createModern();
GraphTraversalSource traversal = graph.traversal();
Driver driver = GremlinDatabase.driver(traversal);
```

Otherwise, the API is the same:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriverSnippets.java#useDriver) -->
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
