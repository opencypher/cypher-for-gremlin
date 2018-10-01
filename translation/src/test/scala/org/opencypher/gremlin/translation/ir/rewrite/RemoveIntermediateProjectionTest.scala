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

import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert._
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class RemoveIntermediateProjectionTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal
    ),
    postConditions = Nil
  )

  @Test
  def singleProjection(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveIntermediateProjection)
      .removes(
        __.project("n")
          .by(__.select("n")))
  }

  @Test
  def multipleProjection(): Unit = {
    assertThat(parse("""
        |MATCH (n)-[r]->(m)
        |RETURN n, m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveIntermediateProjection)
      .removes(
        __.project("n", "m")
          .by(__.select("n"))
          .by(__.select("m")))
  }

  @Test
  def singleAlias(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |RETURN n as a
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveIntermediateProjection)
      .removes(
        __.project("a")
          .by(__.select("n")))
      .removes(__.select("a"))
  }

  @Test
  def multipleAliases(): Unit = {
    assertThat(parse("""
        |MATCH (n)-[r]->(m)
        |RETURN n as a, m as b
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveIntermediateProjection)
      .removes(
        __.project("a", "b")
          .by(__.select("n"))
          .by(__.select("m")))
      .removes(__.select("a"))
      .removes(__.select("b"))
  }
}
