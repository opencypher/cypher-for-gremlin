## Cypher for Gremlin Testware

By default, tests are executed on an embedded TinkerGraph with Gremlin Server Cypher plugin installed [see GremlinServerExternalResource](testware-common/src/main/java/org/opencypher/gremlin/rules/GremlinServerExternalResource.java) using a plugin-compatible client.

To run tests with different client configurations use the `translate` parameter, for example:

    ./gradlew :testware:integration-tests:build -Dtranslate=gremlin
    
To run tests on an external Gremlin Server use the `configPath` parameter, for example:    
    
    ./gradlew :testware:integration-tests:build -DconfigPath="config/cypher-server.yaml"
    
### Examples    
    
#### Run Integration Tests on JanusGraph

* Setup JanusGraph and install [Gremlin Server Cypher Plugin](https://github.com/opencypher/cypher-for-gremlin/tree/master/tinkerpop/cypher-gremlin-server-plugin)
* Drop schema, indexes and constraints
* Run `gradle :testware:integration-tests:testSuite --tests "org.opencypher.gremlin.suites.JanusGraphSuite" -DconfigPath=/path/to/janusgraph-client.yaml`

#### Run Integration Tests on AWS Neptune

* Run `gradle :testware:integration-tests:testSuite --tests "org.opencypher.gremlin.suites.NeptuneSuite" -DconfigPath=/path/to/neptune-client.yaml -Dtranslate=neptune`  
