# Cypher for Gremlin

[![CircleCI](https://circleci.com/gh/opencypher/cypher-for-gremlin.svg?style=shield)](https://circleci.com/gh/opencypher/cypher-for-gremlin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation/badge.svg?style=shield)](https://search.maven.org/#search%7Cga%7C1%7Corg.opencypher.gremlin)

Cypher for Gremlin is a toolkit for users of [Apache TinkerPop™](https://tinkerpop.apache.org/) that allows querying Gremlin databases with [Cypher](https://neo4j.com/docs/developer-manual/current/cypher/), the industry's most widely used [property graph](https://github.com/opencypher/openCypher/blob/master/docs/property-graph-model.adoc) query language defined and maintained by the [openCypher](http://www.opencypher.org) project.

The toolkit is composed of:

- [translation](translation): Cypher to Gremlin translation library for Java
- [tinkerpop/cypher-gremlin-extensions](tinkerpop/cypher-gremlin-extensions): Cypher-specific custom functions and predicates for Gremlin
- [tinkerpop/cypher-gremlin-server-plugin](tinkerpop/cypher-gremlin-server-plugin): Gremlin Server plugin that enables Cypher query processing (also includes Cypher extensions above)
- [tinkerpop/cypher-gremlin-console-plugin](tinkerpop/cypher-gremlin-console-plugin): Gremlin Console plugin that enables client-side translation of Cypher queries or communication with a Cypher-enabled Gremlin Server
- [tinkerpop/cypher-gremlin-server-client](tinkerpop/cypher-gremlin-server-client): Gremlin Server client wrapper that can send Cypher queries to a Cypher-enabled Gremlin Server
- [tinkerpop/cypher-gremlin-neo4j-driver](tinkerpop/cypher-gremlin-neo4j-driver): Neo4j Java API wrapper for users familiar with Neo4j

## Quick Start

The fastest way to start experimenting with Cypher for Gremlin is with the [Gremlin Console Cypher plugin](tinkerpop/cypher-gremlin-console-plugin). Follow the link for installation and usage instructions.

You can also take a look at the [Cypher for Gremlin Demo](https://github.com/neueda/cypher-for-gremlin-demo) project.

## Language Support

With Cypher for Gremlin you can use the following Cypher language features:

- `MATCH` and `OPTIONAL MATCH` with most of the [pattern-matching](https://neo4j.com/docs/developer-manual/current/cypher/syntax/patterns/) syntax, except for variable-length patterns
- `WHERE`, `ORDER BY`, `SKIP`, and `LIMIT` sub-clauses
- `RETURN`, `WITH`, and `UNWIND` projections, including basic support for list and path comprehensions
- `CREATE`, `MERGE`, `SET`, `REMOVE`, and `DETACH DELETE`
- `CASE` expressions
- `UNION` operations

It is not guaranteed that all instances and combinations of the listed features will work. However, the produced translation covers around 70% of the [Cypher Technology Compatibility Kit](https://github.com/opencypher/openCypher/tree/master/tck) and an additional 15% on Gremlin Servers with [Cypher extensions](tinkerpop/cypher-gremlin-extensions). Note that latest TCK M11 covers [Temporal types and functions](https://github.com/opencypher/openCypher/commit/e4b3b7540b506ed2755f0a8116a3f465a5bd9ad9#diff-c26e3fb690fe2673a8f456add0fb3ec8) support of which is currently not implemented in Cypher for Gremlin. Coverage of TCK M10:

<img src="https://docs.google.com/spreadsheets/d/e/2PACX-1vRn3d4ross5VEuEX6m7IZpttIEzzJrtt00UbkDH0UD3A0VAWU7i-ClZU4PSaI3YbDGCQn5vKEX1Hkyr/pubchart?oid=130625852&format=image" width="500">

To see feature support on different platforms (Neptune, JanusGraph), refer to `ExcludeCategory` in [test suites](testware/integration-tests/src/test/java/org/opencypher/gremlin/suites) for each platform. Go to each category description for more details and examples.

You are very welcome to report any [issues](https://github.com/opencypher/cypher-for-gremlin/issues) with the translation that you encounter.

### Major Limitations

* Some functionality is exclusive to Gremlin Servers with [Cypher extensions](tinkerpop/cypher-gremlin-extensions), commonly provided by the [Cypher Gremlin Server plugin](tinkerpop/cypher-gremlin-server-plugin).
* Modification of labels is not supported, because [labels are immutable in Gremlin](https://tinkerpop.apache.org/docs/current/reference/#_multi_label).
* For more details refer to [list of scenarios](../../wiki/Non-translatable-queries) in Cypher TCK without known translation to Gremlin.

See the current [TCK report](testware/tck) for a detailed overview of language coverage.

## Related

* [Gizmo](https://github.com/rebar-cloud/gizmo) is a Web UI that makes it easy to interact with TinkerPop graph databases such as AWS Neptune and Azure CosmosDB with the Cypher query language. Uses [Cypher Gremlin Neo4j Driver](tinkerpop/cypher-gremlin-neo4j-driver) for translation.

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

© Copyright 2018 Neo4j, Inc.

Apache TinkerPop™, TinkerPop, and Apache are registered trademarks of the [Apache Software Foundation](https://www.apache.org/).
