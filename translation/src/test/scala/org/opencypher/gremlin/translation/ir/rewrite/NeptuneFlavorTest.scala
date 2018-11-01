/*
 * Copyright (c) 2018 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.gremlin.translation.ir.rewrite

import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.{P, __}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.{Translator, TranslatorFlavor}
import org.opencypher.gremlin.translation.traversal.DeprecatedOrderAccessor
import org.opencypher.gremlin.translation.traversal.DeprecatedOrderAccessor.{decr, incr}
import org.opencypher.gremlin.translation.walker.NodeUtils

class NeptuneFlavorTest {

  private val flavor = TranslatorFlavor.gremlinServer

  @Test
  def injectWorkaroundTest(): Unit = {
    assertThat(parse("RETURN 1"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .adds(__.V().limit(0))
  }

  @Test
  def limit0Workaround(): Unit = {
    assertThat(parse("CREATE ()"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .removes(__.limit(0))
      .adds(__.select(NONEXISTENT))
  }

  @Test
  def expandListProperties(): Unit = {
    assertThat(parse("CREATE ({foo: [1, 2, 3]})"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .removes(
        __.project("  GENERATED1", "  GENERATED2", "  GENERATED3")
          .by(__.constant(1))
          .by(__.constant(2))
          .by(__.constant(3)))
      .adds(__.property("foo", __.constant(1)))
      .adds(__.property("foo", __.constant(2)))
      .adds(__.property("foo", __.constant(3)))
  }

  @Test
  def multipleLabelsWorkaround(): Unit = {
    assertThat(parse("MATCH (:A:B) RETURN 1"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .adds(__.V().is(P.neq(NONEXISTENT)))
      .keeps(__.hasLabel("A"))
      .keeps(__.hasLabel("B"))
  }

  @Test
  def expandListPropertiesInMerge(): Unit = {
    assertThat(parse("""CREATE (a)
        |SET a.foo = a.foo + [4, 5]
        |RETURN a.foo
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .keeps(
        __.project("  GENERATED2", "  GENERATED3")
          .by(__.constant(4))
          .by(__.constant(5)))
      .adds(__.property("foo", __.constant(4)))
      .adds(__.property("foo", __.constant(5)))
  }

  @Test
  def barrierAfterCount(): Unit = {
    assertThat(parse("MATCH (a) RETURN count(a) > 0"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .keeps(__.count())
      .adds(__.count().barrier())
  }

  @Test
  def aggregateWithSameNameWorkaround(): Unit = {
    assertThat(parse("MATCH p = (:X)-->()-->()-->() RETURN p"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .adds(__.sideEffect(__.aggregate(Tokens.PATH_EDGE + "p")))
  }

  @Test
  def doNotApplyAggregateWithSameNameWorkaround(): Unit = {
    val neptuneFlavor = flavor.extend(
      rewriters = Seq(
        NeptuneFlavor
      ),
      postConditions = Nil)

    assertThat(parse("MATCH p = (:X)-->() RETURN p"))
      .withFlavor(neptuneFlavor)
      .contains(__.aggregate(Tokens.PATH_EDGE + "p"))
      .doesNotContain(__.sideEffect(__.aggregate(Tokens.PATH_EDGE + "p")))
  }

  @Test
  def tinkerPop334WorkaroundAsc(): Unit = {
    assertThat(parse("MATCH (n) RETURN n ORDER BY n.name"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .removes(
        __.by(
          __.select("n")
            .choose(P.neq(NULL), __.choose(__.values("name"), __.values("name"), __.constant("  cypher.null"))),
          Order.asc))
      .adds(
        __.by(
          __.select("n")
            .choose(P.neq(NULL), __.choose(__.values("name"), __.values("name"), __.constant("  cypher.null"))),
          incr))
  }

  @Test
  def tinkerPop334WorkaroundDesc(): Unit = {
    assertThat(parse("MATCH (n) RETURN n ORDER BY n.name DESC"))
      .withFlavor(flavor)
      .rewritingWith(NeptuneFlavor)
      .removes(
        __.by(
          __.select("n")
            .choose(P.neq(NULL), __.choose(__.values("name"), __.values("name"), __.constant("  cypher.null"))),
          Order.desc))
      .adds(
        __.by(
          __.select("n")
            .choose(P.neq(NULL), __.choose(__.values("name"), __.values("name"), __.constant("  cypher.null"))),
          decr))
  }
}
