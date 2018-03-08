## Cypher for Gremlin Testware - Common

By default, tests are executed on an embedded TinkerGraph with Gremlin Server Cypher plugin installed [see GremlinServerExternalResource](src/main/java/org/opencypher/gremlin/rules/GremlinServerExternalResource.java) using a plugin-compatible client.

To run tests with different client configurations use the `translate` parameter, for example:

    ./gradlew :testware:integration-tests:build -Dtranslate=gremlin
    
To run tests on an external Gremlin Server use the `configPath` parameter, for example:    
    
    ./gradlew :testware:integration-tests:build -DconfigPath="config/cypher-server.yaml"
