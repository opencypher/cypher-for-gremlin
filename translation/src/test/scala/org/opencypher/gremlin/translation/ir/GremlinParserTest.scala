/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.gremlin.translation.ir

import org.apache.tinkerpop.gremlin.process.traversal.{Order, Scope}
import org.apache.tinkerpop.gremlin.structure.Column
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality
import org.junit.Test
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.{P, __}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat

class GremlinParserTest {

  @Test
  def allSteps(): Unit = {
    val allSteps = "V()" +
      ".E()" +
      ".addE('edgeLabel')" +
      ".addV()" +
      ".addV('vertexLabel')" +
      ".aggregate('sideEffectKey')" +
      ".and(V().label(), V().label())" +
      ".as('stepLabel')" +
      ".barrier()" +
      ".bothE('edgeLabel1', 'edgeLabel2')" +
      ".by(V().label())" +
      ".by(V().label(), shuffle)" +
      ".cap('sideEffectKey')" +
      ".choose(V().label())" +
      ".choose(V().label(), V().outE(), V().inE())" +
      ".choose(eq('val'), V().outE())" +
      ".choose(eq('val'), V().outE(), V().inE())" +
      ".coalesce(V().label(), V().label(), V().label())" +
      ".constant(1)" +
      ".count()" +
      ".count(local)" +
      ".dedup('dedupLabel1', 'dedupLabel2', 'dedupLabel3')" +
      ".drop()" +
      ".emit()" +
      ".emit(V().label())" +
      ".flatMap(V().label())" +
      ".fold()" +
      ".from('fromStepLabel')" +
      ".group()" +
      ".has('propertyKey')" +
      ".has('propertyKey', eq(1))" +
      ".hasKey('label1', 'label2')" +
      ".hasLabel('label1', 'label2')" +
      ".hasNot('propertyKey')" +
      ".id()" +
      ".identity()" +
      ".inE('label1', 'label2')" +
      ".inV()" +
      ".inject(1, true, 'val', null)" +
      ".is(eq('val'))" +
      ".is(eq(1))" +
      ".is(eq(true))" +
      ".is(gt(1))" +
      ".is(gte(1))" +
      ".is(lt(1))" +
      ".is(lte(1))" +
      ".is(neq(1))" +
      ".is(between(1, 2))" +
      ".is(within(1, true, 'val', null))" +
      ".is(without(1, true, 'val', null))" +
      ".key()" +
      ".label()" +
      ".limit(1)" +
      ".limit(local, 1)" +
      ".local(V().label())" +
      ".loops()" +
      ".map(V().label())" +
      ".math('1+2')" +
      ".max()" +
      ".mean()" +
      ".min()" +
      ".not(V().outE())" +
      ".option('val', V().label())" +
      ".optional(V().label())" +
      ".or(V().outE(), V().inE())" +
      ".order()" +
      ".otherV()" +
      ".outE('label1', 'label2')" +
      ".outV()" +
      ".path()" +
      ".properties('propertyKey1', 'propertyKey2')" +
      ".property('key', 'value')" +
      ".property(list, 'key', 'value')" +
      ".property('key', V().label())" +
      ".property(list, 'key', V().label())" +
      ".project('key1', 'key2', 'key3')" +
      ".range(local, 1, 2)" +
      ".repeat(V().label())" +
      ".select('key1', 'key2', 'key3')" +
      ".select(keys)" +
      ".sideEffect(V().label())" +
      ".simplePath()" +
      ".skip(1)" +
      ".sum()" +
      ".tail(local, 1)" +
      ".times(1)" +
      ".to('toStepLabel')" +
      ".unfold()" +
      ".union(V().outE(), V().inE())" +
      ".until(V().label())" +
      ".value()" +
      ".valueMap()" +
      ".valueMap(true)" +
      ".values('key1', 'key2', 'key3')" +
      ".where(V().label())" +
      ".where(eq(1))"

    val allStepsIr =
      __.V()
        .E()
        .addE("edgeLabel")
        .addV()
        .addV("vertexLabel")
        .aggregate("sideEffectKey")
        .and(__.V().label(), __.V().label())
        .as("stepLabel")
        .barrier()
        .bothE("edgeLabel1", "edgeLabel2")
        .by(__.V().label())
        .by(__.V().label(), Order.shuffle)
        .cap("sideEffectKey")
        .choose(__.V().label())
        .choose(__.V().label(), __.V().outE(), __.V().inE())
        .choose(P.isEq("val"), __.V().outE())
        .choose(P.isEq("val"), __.V().outE(), __.V().inE())
        .coalesce(__.V().label(), __.V().label(), __.V().label())
        .constant(1)
        .count()
        .count(Scope.local)
        .dedup("dedupLabel1", "dedupLabel2", "dedupLabel3")
        .drop()
        .emit()
        .emit(__.V().label())
        .flatMap(__.V().label())
        .fold()
        .from("fromStepLabel")
        .group()
        .has("propertyKey")
        .has("propertyKey", P.isEq(1))
        .hasKey("label1", "label2")
        .hasLabel("label1", "label2")
        .hasNot("propertyKey")
        .id()
        .identity()
        .inE("label1", "label2")
        .inV()
        .inject(1.asInstanceOf[Object], true.asInstanceOf[Object], "val".asInstanceOf[Object], null)
        .is(P.isEq("val"))
        .is(P.isEq(1))
        .is(P.isEq(true))
        .is(P.gt(1))
        .is(P.gte(1))
        .is(P.lt(1))
        .is(P.lte(1))
        .is(P.neq(1))
        .is(P.between(1, 2))
        .is(P.within(1.asInstanceOf[Object], true.asInstanceOf[Object], "val".asInstanceOf[Object], null))
        .is(P.without(1.asInstanceOf[Object], true.asInstanceOf[Object], "val".asInstanceOf[Object], null))
        .key()
        .label()
        .limit(1)
        .limit(Scope.local, 1)
        .local(__.V().label())
        .loops()
        .map(__.V().label())
        .math("1+2")
        .max()
        .mean()
        .min()
        .not(__.V().outE())
        .option("val", __.V().label())
        .optional(__.V().label())
        .or(__.V().outE(), __.V().inE())
        .order()
        .otherV()
        .outE("label1", "label2")
        .outV()
        .path()
        .properties("propertyKey1", "propertyKey2")
        .property("key", "value")
        .property(Cardinality.list, "key", "value")
        .property("key", __.V().label())
        .property(Cardinality.list, "key", __.V().label())
        .project("key1", "key2", "key3")
        .range(Scope.local, 1, 2)
        .repeat(__.V().label())
        .select("key1", "key2", "key3")
        .select(Column.keys)
        .sideEffect(__.V().label())
        .simplePath()
        .skip(1)
        .sum()
        .tail(Scope.local, 1)
        .times(1)
        .to("toStepLabel")
        .unfold()
        .union(__.V().outE(), __.V().inE())
        .until(__.V().label())
        .value()
        .valueMap()
        .valueMap(true)
        .values("key1", "key2", "key3")
        .where(__.V().label())
        .where(P.isEq(1))

    val ir = GremlinParser.parse(allSteps)

    assertThat(ir).isEqualTo(allStepsIr.current())
  }

}
