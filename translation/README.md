# Cypher to Gremlin Translation

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation)

The translation module provides facilities to:
- parse Cypher queries using the Cypher frontend,
- produce a translation to Gremlin,
- transform results to a format that is accepted by a Gremlin Server-based database, Amazon Neptune, or Azure Cosmos DB.

## Getting Started

To add a dependency using Maven:

```xml
<dependency>
    <groupId>org.opencypher.gremlin</groupId>
    <artifactId>translation</artifactId>
    <version>1.0.2</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'org.opencypher.gremlin:translation:1.0.2'
}
```

You can also [build the snapshot](../README.md#development) from source.

## Usage

To translate a Cypher query to a Gremlin query:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#translate) -->
```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
TranslationFacade cfog = new TranslationFacade();
String gremlin = cfog.toGremlinGroovy(cypher);
```

A bit more verbose version of the above, demonstrating several extension points:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#verbose) -->
```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
CypherAst ast = CypherAst.parse(cypher);
Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();
String gremlin = ast.buildTranslation(translator);
```

Note that `Translator` instances are not reusable. A new one has to be created for each `buildTranslation` call. `TranslationFacade` handles this for you.

`Translator` instances support other common translation targets out of the box, like Gremlin bytecode:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#bytecode) -->
```java
Translator<Bytecode, P> translator = Translator.builder()
    .bytecode()
    .build();
```

### Connecting to TinkerPop <3.4.0

Note that by default, translation targets TinkerPop 3.4.0 and uses steps and predicates that are unavailable in earlier 
Gremlin versions, for example [With Step](http://tinkerpop.apache.org/docs/current/reference/#with-step) and [startingWith](http://tinkerpop.apache.org/docs/current/reference/#a-note-on-predicates).
To provide translation suitable for these environments (for example JanusGraph <0.4.0) use `gremlinServer33x` [flavor](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#flavors)

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#translator33x) -->
```java
Translator<String, GroovyPredicate> translator = Translator.builder()
    .gremlinGroovy()
    .build(TranslatorFlavor.gremlinServer33x());
```

### Amazon Neptune

A translator for Amazon Neptune can be configured like so:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#neptune) -->
```java
Translator<String, GroovyPredicate> translator = Translator.builder()
    .gremlinGroovy()
    .inlineParameters()
    .enableMultipleLabels()
    .build(TranslatorFlavor.neptune());
```

### Azure Cosmos DB

A translator for Azure Cosmos DB can be configured like so:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#cosmosdb) -->
```java
Translator<String, GroovyPredicate> translator = Translator.builder()
    .gremlinGroovy()
    .build(TranslatorFlavor.cosmosDb());
```

### Custom Translation

Custom translation targets can be provided by implementing `GremlinSteps`, `GremlinPredicates`, and `GremlinParameters`:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/TranslationSnippets.java#custom) -->
```java
Translator.builder()
    .custom(
        new MyGremlinSteps(),
        new MyGremlinPredicates(),
        new MyGremlinBindings()
    )
    .build();
```

Consult the [Javadoc](https://opencypher.github.io/cypher-for-gremlin/api/1.0.2/java/org/opencypher/gremlin/translation/package-summary.html) for more information.

## Running Cypher

If you want to run queries, not just translate them, you might be interested in the [Cypher client for Gremlin Server](../tinkerpop/cypher-gremlin-server-client).
