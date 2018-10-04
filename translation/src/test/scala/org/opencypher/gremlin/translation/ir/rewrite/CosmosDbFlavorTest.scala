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

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.{Assertions, ThrowableAssert}
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.ir.builder.IRGremlinPredicates
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.ir.helpers.JavaHelpers.objects
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class CosmosDbFlavorTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal,
      RemoveUselessSteps
    ),
    postConditions = Nil
  )

  private val P = new IRGremlinPredicates

  @Test
  def values(): Unit = {
    assertThat(parse("""
        |MATCH (n:N)
        |RETURN n.p
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(CosmosDbFlavor)
      .removes(__.values("p"))
      .adds(__.properties().hasKey("p").value())
  }

  @Test
  def range(): Unit = {
    assertThat(parse("""
        |UNWIND range(1, 3) AS i
        |RETURN i
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(CosmosDbFlavor)
      .removes(
        __.repeat(__.sideEffect(__.loops().is(P.gte(1)).aggregate("  GENERATED1")))
          .until(__.loops().is(P.gt(3)))
      )
      .adds(__.inject(objects(1, 2, 3): _*))
  }

  @Test
  def rangeWithStep(): Unit = {
    assertThat(parse("""
        |UNWIND range(1, 5, 2) AS i
        |RETURN i
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(CosmosDbFlavor)
      .removes(
        __.repeat(
            __.sideEffect(
              __.loops()
                .is(P.gte(1))
                .where(__
                  .math("(_ - 1) % 2")
                  .is(P.isEq(0)))
                .aggregate("  GENERATED1")))
          .until(__.loops().is(P.gt(5)))
      )
      .adds(__.inject(objects(1, 3, 5): _*))
  }

  @Test
  def rangeWithExpression(): Unit = {
    assertThatThrownBy(new ThrowableAssert.ThrowingCallable {
      override def call(): Unit =
        parse("WITH ['a', 'b', 'c'] AS a RETURN range(1, size(a) - 1) as r")
          .translate(flavor.extend(Seq(CosmosDbFlavor)))
    }).hasMessageContaining("Ranges with expressions are not supported")
  }
}
