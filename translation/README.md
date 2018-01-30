# Cypher to Gremlin Translation

The translation module provides facilities to:
- parse Cypher queries using the Cypher frontend,
- produce a translation to Gremlin,
- transform results to a format that is accepted by the target Gremlin Server implementation.

## Usage

To translate a Cypher query to a Gremlin query:

```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
TranslationFacade cfog = new TranslationFacade();
String gremlin = cfog.toGremlin(cypher);
```

A more verbose version of the above, demonstrating several extension points:

```java
String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
CypherAstWrapper ast = CypherAstWrapper.parse(cypher);
Translator<String, StringPredicate> translator = TranslatorFactory.string();
TranslationPlan<String> translationPlan = ast.buildTranslation(translator);
String gremlin = translationPlan.getTranslation();
```

Custom translation targets can be provided by implementing `TranslationBuilder` and `PredicateFactory`:

```java
Translator<String, StringPredicate> translator = new Translator<>(
    new MyTranslationBuilder(),
    new MyPredicateFactory()
); 
```

This is how all the different pieces fit together:

![](assets/translation-module.png)

Consult the published Javadoc for more information.
