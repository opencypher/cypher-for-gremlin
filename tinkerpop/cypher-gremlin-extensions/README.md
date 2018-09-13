# Gremlin Cypher Extensions

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-extensions/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-extensions)

This is a collection of custom functions and predicates that can be added to Gremlin Server classpath. These functions and predicates provide some functionality native to Cypher for use in Gremlin.

The [translation module](../../translation) relies on these extensions to produce translations for:
- [List](https://neo4j.com/docs/developer-manual/current/cypher/syntax/lists/) access by non-constant index
- [Map](https://neo4j.com/docs/developer-manual/current/cypher/syntax/maps/) access
- String concatenation and non-constant plus operator
- The following [functions](https://neo4j.com/docs/developer-manual/current/cypher/functions/): `percentileCont`, `percentileDisc`, `toBoolean`, `toFloat`, `toInteger`, `toString`
- Some instances of `properties`, `size`

## Usage

The easiest way to use this module is by installing the [Gremlin Server Cypher plugin](../cypher-gremlin-server-plugin) on the target Gremlin Server. The plugin includes all of the extensions and registers them on the Server.

Alternatively, add [CustomPredicate.java](src/main/java/org/opencypher/gremlin/traversal/CustomPredicate.java) and [CustomFunctions.java](src/main/java/org/opencypher/gremlin/traversal/CustomFunctions.java) to Gremlin Groovy script engine.
