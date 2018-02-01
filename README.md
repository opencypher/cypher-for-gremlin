# Cypher for Gremlin

Cypher for Gremlin is a toolkit for users of [Apache TinkerPop™](https://tinkerpop.apache.org/) that allows querying Gremlin databases with [Cypher](https://neo4j.com/docs/developer-manual/current/cypher/), the industry's most widely used [property graph](https://github.com/opencypher/openCypher/blob/master/docs/property-graph-model.adoc) query language defined and maintained by the [openCypher](http://www.opencypher.org) project.

The toolkit is composed of:

- [translation](translation): Cypher to Gremlin translation library for Java
- [plugin/server-plugin](plugin/server-plugin): Gremlin Server plugin that enables Cypher query processing
- [plugin/console-plugin](plugin/console-plugin): Gremlin Console plugin that can send Cypher queries to a remote
- Gremlin Console plugin that enables client-side translation of Cypher queries or communication with a Cypher-enabled Gremlin Server

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
`CREATE`         | `DISTINCT`  | `()-[*n..m]-()`                | type conversions
`MERGE`          |             | <code>[x IN … &#124; …]</code> | string matching
`SET`            |             |                                |
`DETACH DELETE`  |             |                                |

It is not expected, however, that any combination of the listed clauses, patterns, or functions will work. You are welcome to report any [issues](https://github.com/opencypher/cypher-for-gremlin/issues) with the translation that you encounter.

See the current TCK report for a detailed overview of language coverage.

## Development

To build and run Cypher for Gremlin you need Java 8.
The project is built using [Gradle](https://gradle.org/) or the provided wrapper.

To build and run unit and integration tests:

```
./gradlew build
```

To automatically fix formatting errors:

```
./gradlew spotlessApply
```

## License

The project is licensed under the Apache Software License, Version 2.0

## Copyright

© Copyright 2018 Neo4j, Inc.

Apache TinkerPop™, TinkerPop, and Apache are registered trademarks of the [Apache Software Foundation](https://www.apache.org/).
