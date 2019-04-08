# Cypher for Gremlin

[![CircleCI](https://circleci.com/gh/opencypher/cypher-for-gremlin.svg?style=shield)](https://circleci.com/gh/opencypher/cypher-for-gremlin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation/badge.svg?style=shield)](https://search.maven.org/#search%7Cga%7C1%7Corg.opencypher.gremlin)

Cypher for Gremlin is a toolkit for users of [Apache TinkerPop™](https://tinkerpop.apache.org/) that allows querying Gremlin databases with [Cypher](https://neo4j.com/docs/developer-manual/current/cypher/), the industry's most widely used [property graph](https://github.com/opencypher/openCypher/blob/master/docs/property-graph-model.adoc) query language defined and maintained by the [openCypher](http://www.opencypher.org) project.

Cypher query is translated to one of Gremlin representations (Gremlin Groovy string, Traversal object or Gremlin bytecode):

<img src="https://drive.google.com/uc?export=view&id=1HPxZrNkJxrmnd8BlB8YQqX5-pc9TWKUn" width="600" />

## Table of Contents

* [Overview](#overview)
* [Highlights](#highlights)
* [Quick Start](#quick-start)
* [Toolkit](#toolkit)
* [Language Support](#language-support)
* [Related](#related)
* [Implementation](#implementation)
* [Development](#development)
* [How to contribute](#how-to-contribute)
* [License](#license)
* [Copyright](#copyright)

## Overview

<img src="https://drive.google.com/uc?export=view&id=1jSZH9sJtm4sBu8ZoAaEdb7CLXzRWdUlq" />

## Highlights

### Gremlin Console

[Gremlin Console plugin](tinkerpop/cypher-gremlin-console-plugin) that enables client-side translation of Cypher queries or communication with a Cypher-enabled Gremlin Server (click to play):

<img src="https://drive.google.com/uc?export=view&id=1vncDfbO8o9Ef060SFOBmlQpt4v7etGrJ" />

### Gremlin Server Client

[Gremlin Server client](tinkerpop/cypher-gremlin-server-client) wrapper that can send Cypher queries to a Cypher-enabled Gremlin Server or translate Cypher queries to Gremlin on client side, and send translated query to servers:

<!-- [freshReadmeSource](testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#demo) -->
```java
String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";

Cluster cluster = Cluster.open(configuration);
Client gremlinClient = cluster.connect();

// Server has Gremlin Server plugin installed
// Send Cypher to server
CypherGremlinClient cypherGremlinClient = CypherGremlinClient.plugin(gremlinClient);
List<Map<String, Object>> cypherResults = cypherGremlinClient.submit(cypher).all();

// Client side translation
// Send Gremlin to server
CypherGremlinClient translatingGremlinClient = CypherGremlinClient.translating(gremlinClient);
List<Map<String, Object>> gremlinResults = translatingGremlinClient.submit(cypher).all();

assertThat(cypherResults).isEqualTo(gremlinResults);

```

### Gremlin Neo4j Driver

[Implementation of Neo4j API interfaces](tinkerpop/cypher-gremlin-neo4j-driver) for users familiar with Neo4j:

<!-- [freshReadmeSource](testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinNeo4jDriverSnippets.java#demo) -->
```java
Driver driver = GremlinDatabase.driver(uri);

try (Session session = driver.session()) {
    StatementResult result = session.run("MATCH (n) RETURN count(n) as count");
    int n = result.single().get("count").asInt();

    assertThat(n).isEqualTo(0); // 0
}
```

### Gremlin Server Plugin

[Gremlin Server plugin](tinkerpop/cypher-gremlin-server-plugin) that enables Cypher query processing on Gremlin Server. For example connect using [Gremlin-JavaScript](http://tinkerpop.apache.org/docs/current/reference/#gremlin-javascript) 2.7.0 by setting `processor` to `cypher`:

```js
// npm install gremlin@2.7.0

const gremlin = require('gremlin');

const client = gremlin.createClient(8182, "localhost", {processor: "cypher"})

client.execute('MATCH (n) RETURN count(n)', (err, results) => {
    console.log(results)
});
```

## Quick Start

1. Run [Docker images](docker)
2. Start experimenting with Cypher for Gremlin with the [Gremlin Console Cypher plugin](tinkerpop/cypher-gremlin-console-plugin). Follow the link for installation and usage instructions.
3. For API usage take a look at the [Cypher for Gremlin Demo](https://github.com/neueda/cypher-for-gremlin-demo) project.

## Toolkit

The toolkit is composed of:

- [translation](translation): Cypher to Gremlin translation library for Java
- [tinkerpop/cypher-gremlin-extensions](tinkerpop/cypher-gremlin-extensions): Extensions to Gremlin to enable full support for Cypher functionality
- [tinkerpop/cypher-gremlin-server-plugin](tinkerpop/cypher-gremlin-server-plugin): Gremlin Server plugin that enables Cypher query processing (also includes extensions above)
- [tinkerpop/cypher-gremlin-console-plugin](tinkerpop/cypher-gremlin-console-plugin): Gremlin Console plugin that enables client-side translation of Cypher queries or communication with a Cypher-enabled Gremlin Server
- [tinkerpop/cypher-gremlin-server-client](tinkerpop/cypher-gremlin-server-client): Gremlin Server client wrapper that can send Cypher queries to a Cypher-enabled Gremlin Server
- [tinkerpop/cypher-gremlin-neo4j-driver](tinkerpop/cypher-gremlin-neo4j-driver): Neo4j Java API wrapper for users familiar with Neo4j

## Language Support

With Cypher for Gremlin you can use the following Cypher language features:

- `MATCH` and `OPTIONAL MATCH`
- `WHERE`, `ORDER BY`, `SKIP`, and `LIMIT` sub-clauses
- `RETURN`, `WITH`, and `UNWIND` projections, including basic support for list and path comprehensions
- `CREATE`, `MERGE`, `SET`, `REMOVE`, and `(DETACH) DELETE`
- `CASE` expressions
- `UNION (ALL)` operations
- `CALL` procedures

It is not guaranteed that all instances and combinations of the listed features will work. However, in addition to [integration tests](testware), correctness of translation is verified by the [Cypher Technology Compatibility Kit](https://github.com/opencypher/openCypher/tree/master/tck) (TCK). The TCK is an openCypher artifact and contains a comprehensive set of test scenarios validating different features of the Cypher language.

Coverage of TCK M13 ([excluding Temporal Types](tinkerpop/cypher-gremlin-extensions#temporal-types)) on TinkerGraph:

- 75% of the scenarios are supported with common Gremlin steps
- Additional 15% with [extensions](tinkerpop/cypher-gremlin-extensions) to Gremlin to enable full support for Cypher functionality
- See [latest TCK Report](https://opencypher.github.io/cypher-for-gremlin/test-reports/1.0.0/cucumber-html-reports/overview-features.html) for a detailed overview of language coverage.
- To see feature support on different platforms (Neptune, JanusGraph, Cosmos DB), refer to [Gremlin implementations documentation](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations).

<img src="https://docs.google.com/spreadsheets/d/e/2PACX-1vRn3d4ross5VEuEX6m7IZpttIEzzJrtt00UbkDH0UD3A0VAWU7i-ClZU4PSaI3YbDGCQn5vKEX1Hkyr/pubchart?oid=130625852&format=image" width="500">

You are very welcome to report any [issues](https://github.com/opencypher/cypher-for-gremlin/issues) with the translation that you encounter.

## Related

* [Gizmo](https://github.com/rebar-cloud/gizmo) is a Web UI that makes it easy to interact with TinkerPop graph databases such as AWS Neptune and Azure CosmosDB with the Cypher query language. Uses [Cypher Gremlin Neo4j Driver](tinkerpop/cypher-gremlin-neo4j-driver) for translation.

## Implementation

<img src="https://drive.google.com/uc?export=view&id=1-7jcZiVaNBfP1-6S9eFemu_NhazIBnqG" />

The translation process uses a reasonably sophisticated and flexible approach. Cypher query is parsed by the [openCypher Frontend](https://github.com/opencypher/front-end) and translated to an [internal representation](translation/src/main/scala/org/opencypher/gremlin/translation/ir/model) by the Cypher for Gremlin. The internal representation is transformed by a set of [rewriters](translation/src/main/scala/org/opencypher/gremlin/translation/ir/rewrite) to adapt the query for system specifics of different Gremlin implementations (JanusGraph, Cosmos DB, AWS Neptune), then converted to one of Gremlin representations (Gremlin Groovy string, Traversal object or Gremlin bytecode).

## Development

To build and run Cypher for Gremlin you need Java 8.
The project is built using [Gradle](https://gradle.org/) or the provided wrapper.

To build and run unit and integration tests:

```
./gradlew build
```

To automatically fix formatting errors in your changes:

```
./gradlew spotlessApply
```

## How to contribute

We would love to find out about any [issues](https://github.com/opencypher/cypher-for-gremlin/issues) you encounter and are happy to accept contributions following a Contributors License Agreement (CLA) signature as per the process outlined in our [contribution guidelines](CONTRIBUTING.md).

## License

The project is licensed under the Apache Software License, Version 2.0

## Copyright

© Copyright 2018-2019 Neo4j, Inc.

Apache TinkerPop™, TinkerPop, and Apache are registered trademarks of the [Apache Software Foundation](https://www.apache.org/).
