# Cypher to Gremlin Translation

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/translation)

The translation module provides facilities to:
- parse Cypher queries using the Cypher frontend,
- produce a translation to Gremlin,
- transform results to a format that is accepted by the target Gremlin Server implementation.

## Getting Started

To add a dependency using Maven:

```xml
<dependency>
    <groupId>org.opencypher.gremlin</groupId>
    <artifactId>translation</artifactId>
    <version>0.9.3</version>
</dependency>
```

To add a dependency using Gradle:

```
dependencies {
  compile 'org.opencypher.gremlin:translation:0.9.3'
}
```

You can also [build the snapshot](../README.md#development) from source.

## Usage

To translate a Cypher query to a Gremlin query:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/Translation.java#translate) -->
```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
TranslationFacade cfog = new TranslationFacade();
String gremlin = cfog.toGremlinGroovy(cypher);
```

A bit more verbose version of the above, demonstrating several extension points:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/Translation.java#verbose) -->
```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
CypherAstWrapper ast = CypherAstWrapper.parse(cypher);
Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();
String gremlin = ast.buildTranslation(translator);
```

Note that `Translator` instances are not reusable. A new one has to be created for each `buildTranslation` call. `TranslationFacade` handles this for you.

`Translator` instances support other common translation targets out of the box, like Gremlin bytecode:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/Translation.java#bytecode) -->
```java
Translator<Bytecode, P> translator = Translator.builder()
    .bytecode()
    .build();
```

Some translation targets can be customized with a flavor, like Azure Cosmos DB:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/Translation.java#cosmosdb) -->
```java
Translator<String, GroovyPredicate> translator = Translator.builder()
    .gremlinGroovy()
    .build(TranslatorFlavor.cosmosDb());
```

Custom translation targets can be provided by implementing `GremlinSteps`, `GremlinPredicates`, and `GremlinParameters`:

<!-- [freshReadmeSource](../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/Translation.java#custom) -->
```java
Translator.builder()
    .custom(
        new MyGremlinSteps(),
        new MyGremlinPredicates(),
        new MyGremlinBindings()
    )
    .build();
```

Consult the Javadoc for more information.

## Running Cypher

If you want to run queries, not just translate them, you might be interested in the [Cypher client for Gremlin Server](../tinkerpop/cypher-gremlin-server-client).
