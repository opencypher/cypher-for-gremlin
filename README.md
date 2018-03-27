# Cypher for Gremlin

[![CircleCI](https://circleci.com/gh/opencypher/cypher-for-gremlin.svg?style=shield)](https://circleci.com/gh/opencypher/cypher-for-gremlin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation/badge.svg?style=shield)](https://search.maven.org/#search%7Cga%7C1%7Corg.opencypher.gremlin)

Cypher for Gremlin is a toolkit for users of [Apache TinkerPop™](https://tinkerpop.apache.org/) that allows querying Gremlin databases with [Cypher](https://neo4j.com/docs/developer-manual/current/cypher/), the industry's most widely used [property graph](https://github.com/opencypher/openCypher/blob/master/docs/property-graph-model.adoc) query language defined and maintained by the [openCypher](http://www.opencypher.org) project.

The toolkit is composed of:

- [translation](translation): Cypher to Gremlin translation library for Java
- [tinkerpop/cypher-gremlin-server-plugin](tinkerpop/cypher-gremlin-server-plugin): Gremlin Server plugin that enables Cypher query processing
- [tinkerpop/cypher-gremlin-console-plugin](tinkerpop/cypher-gremlin-console-plugin): Gremlin Console plugin that enables client-side translation of Cypher queries or communication with a Cypher-enabled Gremlin Server
- [tinkerpop/cypher-gremlin-server-client](tinkerpop/cypher-gremlin-server-client): Gremlin Server client wrapper that can send Cypher queries to a Cypher-enabled Gremlin Server
- [tinkerpop/cypher-gremlin-neo4j-driver](tinkerpop/cypher-gremlin-neo4j-driver): Neo4j Java API wrapper for users familiar with Neo4j

## Language Support

With Cypher for Gremlin you can use the following Cypher language features:

Clauses          | Other       | Patterns                       | Functions
-----------------|-------------|--------------------------------|--------------------------
`MATCH`          | `ON CREATE` | `(n:L {k: ‘v’})`               | `avg`, `collect`, `count`
`RETURN`         | `ON MATCH`  | `()-->()`                      | `max`, `min`, `sum`
`UNWIND`         | `WHERE`     | `()--()`                       | `keys`, `labels`
`OPTIONAL MATCH` | `ORDER BY`  | `()-[r:L {k: ‘v’}]-()`         | `nodes`, `relationships`
`WITH`           | `SKIP`      | `(n)-[r]-(m)`                  | `size`
`UNION`          | `LIMIT`     | `()-[]-()-[]-()`               | `type`, `exists`
`CREATE`         | `DISTINCT`  | <code>[x IN … &#124; …]</code> | type conversions
`MERGE`          |             |                                | string matching
`SET`            |             |                                |
`DETACH DELETE`  |             |                                |

It is not guaranteed, however, that all combinations of the listed clauses, patterns, or functions will work. You are welcome to report any [issues](https://github.com/opencypher/cypher-for-gremlin/issues) with the translation that you encounter.

### Major Limitations

* Some functionality is exclusive to Gremlin Servers with the [Cypher plugin](tinkerpop/cypher-gremlin-server-plugin) installed, including:
  - Accessing [list](https://neo4j.com/docs/developer-manual/current/cypher/syntax/lists/) elements by index
  - [Literal maps](https://neo4j.com/docs/developer-manual/current/cypher/syntax/maps/)
  - [List comprehensions](https://neo4j.com/docs/developer-manual/current/cypher/syntax/lists/#cypher-list-comprehension)
  - [Returning named paths](https://neo4j.com/docs/developer-manual/current/cypher/clauses/match/#named-paths)
  - The following [functions](https://neo4j.com/docs/developer-manual/current/cypher/functions/): `length()`, `nodes()`, `relationships()`, `size()`, `toString()`, `toBoolean()`, `toInteger()`, `toFloat()`
* Modification of labels is not supported, because [labels are immutable in Gremlin](https://tinkerpop.apache.org/docs/current/reference/#_multi_label).

See the current [TCK report](testware/tck) for a detailed overview of language coverage.

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
