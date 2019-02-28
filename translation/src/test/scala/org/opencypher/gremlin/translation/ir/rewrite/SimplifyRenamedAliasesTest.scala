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
package org.opencypher.gremlin.translation.ir.rewrite

import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.{P, __}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.ir.model.Vertex
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class SimplifyRenamedAliasesTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal,
      RemoveMultipleAliases
    ),
    postConditions = Nil
  )

  @Test
  def whereMatchPattern(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |WHERE (n)-->(:L)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifyRenamedAliases)
      .removes(
        __.V()
          .as("  GENERATED3")
          .where(__.select("  GENERATED3").where(P.isEq("n")))
          .as("  cypher.path.start.GENERATED4")
      )
      .adds(
        __.select("n")
          .as("  cypher.path.start.GENERATED4")
      )
  }

  @Test
  def whereNonStartMatchPattern(): Unit = {
    assertThat(parse("""
        |MATCH (n)-->(m)
        |WHERE (m)-->(:L)
        |RETURN m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifyRenamedAliases)
      .removes(
        __.V()
          .as("  GENERATED4")
          .where(__.select("  GENERATED4").where(P.isEq("m")))
          .as("  cypher.path.start.GENERATED5")
      )
      .adds(
        __.select("m")
          .is(P.neq(NULL))
          .as("  cypher.path.start.GENERATED5")
      )
  }

  @Test
  def matchPath(): Unit = {
    val ast = parse("""MATCH (o:Officer)
        |WHERE o.name = 'PELAGOS YACHTS LIMITED'
        |MATCH path = (o)-[r]->(:Entity)
        |RETURN path LIMIT 100""".stripMargin)

    val steps = ast.translate(
      flavor.extend(
        rewriters = Seq(
          SimplifyRenamedAliases
        ),
        postConditions = Nil
      )
    )
    val newTraversalCount = steps.count(_ == Vertex)

    assertThat(newTraversalCount).isEqualTo(1)
  }
}
