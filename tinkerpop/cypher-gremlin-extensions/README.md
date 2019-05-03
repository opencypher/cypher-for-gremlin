# Cypher and Gremlin differences

This page describes differences between Cypher and Gremlin, most of which could be addressed by [installing Gremlin Extensions for Cypher Support](#extensions).

* [75% of the TCK scenarios](../../README.md#language-support) are supported with translation to common Gremlin steps     
* Enabling fuller Cypher support
  * [Gremlin Extensions for Cypher Support](#extensions)
  * [Translation Workarounds](#translation-workarounds)
* Cypher features that cannot be supported  
  * [Non-translatable queries](#non-translatable-queries)
  
You are very welcome to [suggest](https://github.com/opencypher/cypher-for-gremlin/issues) better translation or workaround.

<span id="extensions">
  
## Extensions to Gremlin to enable full support for Cypher functionality

Some functionality is exclusive to Gremlin Servers with Gremlin Extensions for Cypher Support, commonly provided by the [Cypher Gremlin Server plugin](https://github.com/opencypher/cypher-for-gremlin/tree/master/tinkerpop/cypher-gremlin-server-plugin). For example, [functions](https://github.com/opencypher/cypher-for-gremlin/blob/master/tinkerpop/cypher-gremlin-extensions/src/main/java/org/opencypher/gremlin/traversal/CustomFunctions.java#L42) and [predicates](https://github.com/opencypher/cypher-for-gremlin/blob/master/tinkerpop/cypher-gremlin-extensions/src/main/java/org/opencypher/gremlin/traversal/CustomPredicate.java#L24) that are not available in Gremlin. Note that with or without extensions, translation does not use lambdas, as it is [considered bad practice](http://tinkerpop.apache.org/docs/current/reference/#a-note-on-lambdas).

For examples, search for [tests](https://github.com/opencypher/cypher-for-gremlin/tree/master/testware/integration-tests/src/test/java/org/opencypher/gremlin/queries) with category `org.opencypher.gremlin.groups.SkipExtensions`.

### Installation

**⚠ Note**: Gremlin Extensions for Cypher Support are not available for [cloud implementations](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations) like AWS Neptune or Cosmos DB.    

The easiest way to use this module is by installing the [Gremlin Server Cypher plugin](../cypher-gremlin-server-plugin) on the target Gremlin Server. The plugin includes all of the extensions and registers them on the Server.

Alternatively, add [CustomPredicate.java](src/main/java/org/opencypher/gremlin/traversal/CustomPredicate.java) and [CustomFunctions.java](src/main/java/org/opencypher/gremlin/traversal/CustomFunctions.java) to Gremlin Groovy script engine.

### Usage

If extensions are installed on a target server and translation happens on the client side, extensions should be explicitly enabled on the client so the resulting query can use it. For example:

* Java API:
    ```java
    Translator<String, GroovyPredicate> translator = Translator.builder()
        .gremlinGroovy()
        .enableCypherExtensions()
        .build(TranslatorFlavor.gremlinServer());
    ```
* Console:
    ```
    :remote connect opencypher.gremlin conf/remote-objects.yaml translate gremlin+cfog_server_extensions
    ```

**⚠ Note** To include Gremlin in Cypher query see [Gremlin Function](../cypher-gremlin-server-client#gremlin-function).    
### Custom Functions

Functions that are present in Cypher but not in Gremlin:

* Type Conversion [functions](https://neo4j.com/docs/cypher-manual/current/functions/scalar/): `toString`, `toBoolean`, `toInteger`, `toFloat`
* [String functions](https://neo4j.com/docs/cypher-manual/current/functions/string/): `reverse`, `substring`, `trim`, `toUpper`, `toLower`...
* Percentile functions: [percentileCont](https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-percentilecont), [percentileDisc](https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-percentiledisc)
* [round](https://neo4j.com/docs/cypher-manual/current/functions/mathematical-numeric/#functions-round) function

### Queries that require type information

There are no functions or predicates to get the type of object in Gremlin. However, depending on the type of object, Gremlin steps required to achieve certain functionality might be different. For example, when accessing an element by index:

```cypher
WITH $p AS unknown
RETURN unknown[$k] AS r
```

Because `p` and `k` are non constant values (parameters) type is unknown to parser. Depending on type translation to Gremlin could be:

* `values($k)` for vertex and edge
* `select($k)` for maps
* `range(Scope.local, $k, $k + 1)` for lists
* `constant(null)` if `k` or `p` are null

As Gremlin will throw an exception if the step does not match object type, custom functions are used for the unknown type when:

* Accessing element by index
* [properties function](https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-properties)
* [size function](https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-size)

If neither type information and extensions are not available, CfoG relies on "best guess" approach.

### Plus operator

In Cypher, plus operator works with numbers, strings and arrays:

```cypher
RETURN 1 + 2; // 3
RETURN 'a' + 'b'; // "ab"
RETURN [1, 2]+[3,4]; // [1, 2, 3, 4]
RETURN [1, 2]+3; // [1, 2, 3]
```

In Gremlin different steps are required for each type:

* [Math](http://tinkerpop.apache.org/docs/current/reference/#math-step) step for numbers
* `.union(select('list1').unfold(), select('list2').unfold()).fold())` for collections
* String concatenation is not supported in Gremlin

If type information is unknown (or on string concatenation) - custom function is used. If Gremlin Extensions for Cypher Support are not installed, translation falls back to number operator.

### Copying Properties from node to node

```cypher
MATCH (n:FROM)-[r]->(m:TO) SET m=n RETURN m
```

Gremlin [AddProperty step](http://tinkerpop.apache.org/docs/current/reference/#addproperty-step) only allows setting a single value, where the property name is constant. It is unknown how to copy all properties from one element to another.

## Translation Workarounds

### Null handling

* Gremlin has no concept of `null`
  ```groovy
  g.V().has('name', 'lop').values('notExising')
  // empty results

  g.V().has('name', 'lop').project('p').by(values('notExising'))
  // The provided traverser does not map to a value: v[3]->[PropertiesStep([notExising],value)]
  ```
* `null` value produces NullPointerException `g.inject(null)` 
* To represent Cypher `null` value, string token `"  cypher.null"` is used
* To produce `null` values: `.choose(traversal, traversal, "  cypher.null")`
* Null guards are added to translation: `choose("  cypher.null", traversal)`

### Throwing exception

There is no [known way](https://stackoverflow.com/questions/53734954/how-can-i-return-meaningful-errors-in-gremlin) to throw custom an exception from Gremlin traversal. To achieve runtime validation (for example deleting nodes that still have relationships) custom function is used.

### Variable length paths with loops

Currently is implemented using [`repeat`](http://tinkerpop.apache.org/docs/current/reference/#repeat-step), `emit` and
`times` step combination, that works in generic cases, but fails when graph contains loops.

For example nodes `a` and `b`, `b` contains self-loop:

```cypher
CREATE (a:a)-[:knows]->(b:b)
CREATE (b)-[:knows]->(b)
```

To get all paths with length from 1 to 4:

```cypher
MATCH p = (a:a)-[:knows*1..4]->(b) RETURN p
```

Expected result is:

```
["a", "knows", "b"],
["a", "knows", "b", "knows", "b"]
```

Current translation (simplified):

```groovy
g.V().as('a').hasLabel('a').
        emit(__.loops().is(gte(1))).
        repeat(__.outE('knows').inV()).
        times(4)
```

Result is:

```
["a", "knows", "b"],
["a", "knows", "b", "knows", "b"],
["a", "knows", "b", "knows", "b", "knows", "b"],
["a", "knows", "b", "knows", "b", "knows", "b", "knows", "b"]
```

## Non-translatable queries

These are queries and [TCK scenarios](https://github.com/opencypher/cypher-for-gremlin/tree/master/testware/tck) in Cypher TCK without known translation to Gremlin. You are very welcome to [suggest](https://github.com/opencypher/cypher-for-gremlin/issues) translation or workaround.

### Label modification

TinkerPop3 Documentation documentation [states](https://tinkerpop.apache.org/docs/current/reference/#_the_graph_structure): 

> Vertices are allowed a single **immutable** string label

In Cypher it is [possible](https://neo4j.com/docs/developer-manual/current/cypher/clauses/set/#set-set-a-label-on-a-node) to modify label on a node.

Following TCK scenarios rely on thact feature:

* LabelsAcceptance,"Adding a single label"
* LabelsAcceptance,"Ignore space before colon"
* LabelsAcceptance,"Ignoring intermediate whitespace 1"
* LabelsAcceptance,"Removing a label"
* LabelsAcceptance,"Removing a non-existent label"
* MergeNodeAcceptance,"Merge node with label add label on create"
* MergeNodeAcceptance,"Merge node with label add label on match when it exists"
* MergeNodeAcceptance,"Should be able to set labels on match and on create"
* MergeNodeAcceptance,"Should be able to set labels on match"
* NullAcceptance,"Ignore null when removing label"
* NullAcceptance,"Ignore null when setting label"
* RemoveAcceptance,"Remove a single label"
* SetAcceptance,"Add a label to a node"
* RemoveAcceptance,"Remove multiple labels"
* SetAcceptance,"Add a label to a node"
* LabelsAcceptance,"Adding multiple labels"

### Comparability

In Cypher, it is possible to compare values with different types:

```cypher
UNWIND [1, 'string'] AS x RETURN max(x) // returns 1
```

Gremlin traversal fails when comparing values with different types:

```groovy
g.inject(1).inject('string').max()  // fails java.lang.Integer cannot be cast to java.lang.String
```

Following TCK scenarios rely on that feature:

* Aggregation,"`max()` over list values"
* Aggregation,"`max()` over mixed numeric values"
* Aggregation,"`max()` over mixed values"
* Aggregation,"`max()` over strings"
* Aggregation,"`min()` over list values"
* Aggregation,"`min()` over mixed values"
* Aggregation,"`min()` over strings"
* Comparability,"Comparing strings and integers using > in a OR'd predicate "
* Comparability,"Comparing strings and integers using > in an AND'd predicate"

### Temporal Types

There is no common support of Temporal Types in Gremlin, so each [implementation](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations) may handle it differently. It is possible to use `java.util.Date` to represent this in JanusGraph and inmemory TinkerGraph. However, it would require lots of custom code for:

* Creation and parsing of Temporal Types
* Property access
* Arithmetics
* Comparison
  - Custom predicates for all comparisons to account Temporal Types
* Additional functions like `truncate`

### Filtering elements

In query translated by Cypher for Gremlin return elements are normalized depending on element type. When element [type is unknown](#queries-that-require-type-information), normalization is not possible.

```
MATCH (n)-[r]->(m)
RETURN [n, r, m] AS r
```

Following TCK scenarios rely on that feature:

* MatchAcceptance2.Projecting a list of nodes and relationships
* MatchAcceptance2.Projecting a map of nodes and relationships

### Path equality

In Cypher direction of the traversed relationship is not significant for path equality. Path comparison works different in Gremlin and considers direction.

Following TCK scenarios rely on that feature:

* PathEquality.Direction of traversed relationship is not significant for path equality, simple

### Creating big ranges

There is no known simple way to create numeric range in Gremlin ([Range](http://tinkerpop.apache.org/docs/current/reference/#range-step)) step has different use).

Current implementation of numeric `range` in Cypher for Gremlin timeouts when creating a big range (over 10000 elements):

```groovy
// range(1000000, 2000000)

g.inject('start').
        repeat(sideEffect(loops().
                is(gte(1000000)).
                aggregate('range'))).
        until(loops().is(gt(2000000))).
        select('range')
```

Following TCK scenario rely on that feature:

* AggregationAcceptance.No overflow during summation
    - Expects range from `1000000` to `2000000`

### Queries with expression in parameter

Some Gremlin steps support only constant values. For example [Skip](http://tinkerpop.apache.org/docs/current/reference/#skip-step) step. No known way to pass dynamic or expression variables.

```cypher
MATCH (n) RETURN n SKIP toInteger(rand()*9)
```

Following TCK scenario rely on that feature:

* SkipLimitAcceptanceTest.SKIP with an expression that does not depend on variables
