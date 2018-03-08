## openCypher testware common

By default, tests are executed on embedded TinkerGraph with Gremlin Server Cypher Plugin installed [see GremlinServerExternalResource](src/main/java/org/opencypher/gremlin/rules/GremlinServerExternalResource.java).

To run test with Client side translation use `translate` parameter, for example:

    ./gradlew :testware:integration-tests:build -Dtranslate=gremlin
    
To run tests on external Gremlin Server use `configPath` parameter, for example:    
    
    ./gradlew :testware:integration-tests:build -DconfigPath="config/cypher-server.yaml"
