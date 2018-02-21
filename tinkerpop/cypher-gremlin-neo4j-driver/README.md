# Gremlin Neo4j Java Driver

CfoG implementation of Neo4j driver interfaces.

Could be used as drop in replacement to test Gremlin supported databases using [Neo4j Java API](https://neo4j.com/developer/java/). 

## Differences from Neo4j

* Transaction functionality are not supported yet, depends on target database
* `SummaryCounters`, `StatementType` and `TypeSystem` are not supported

## Usage

Entry point `org.opencypher.GremlinDatabase` allows to create drivers to Gremlin server (like `org.neo4j.driver.v1.GraphDatabase`)

```java
Driver driver = GremlinDatabase.driver("//localhost:8182");
```

In case of advanced Gremlin configuration its possible to create driver from `org.apache.tinkerpop.gremlin.driver.Cluster`

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriver.java#createDriver) -->
```java
Cluster cluster1 = Cluster.build()
    .enableSsl(true)
    .addContactPoints("192.168.0.145")
    //...
    .create();
Driver driver1 = GremlinDatabase.driver(cluster1);

//...

Cluster cluster2 = Cluster.open(pathToGremlinConfiguration);

Driver driver2 = GremlinDatabase.driver(cluster2);
```

By default Cypher queries will be sent without translation, expecting [Cypher plugin](../cypher-gremlin-server-plugin) to
be installed. Its possible to configure Driver, to perform translation to Gremlin before sending it to server:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriver.java#createConfiguration) -->
```java
Config config = Config.build()
    .withTranslation()
    .toConfig();

String uri = "//localhost:" + port;
Driver driver = GremlinDatabase.driver(uri, config);
```

Its possible to work with driver using [Neo4j Java API](https://neo4j.com/developer/java/):

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
