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
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.{P, __}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class GroupStepFiltersTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineMapTraversal
    ),
    postConditions = Nil
  )

  @Test
  def singlePattern(): Unit = {
    assertThat(parse("""
        |MATCH (n:N)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.where(__.select("n").hasLabel("N")))
      .keeps(__.hasLabel("N"))
  }

  @Test
  def singleWhere(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |WHERE n.p = 'n' AND 1 <> 2
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(
          __.and(
            __.select("n").values("p").is(P.isEq("n")),
            __.constant(1).is(P.neq(2))
          )))
      .adds(
        __.has("p", P.isEq("n"))
          .where(__.constant(1).is(P.neq(2)))
      )
  }

  @Test
  def multiplePatterns(): Unit = {
    assertThat(parse("""
        |MATCH (n:N {p: 'n'})-[r:R {p: 'r'}]->(m:M {p: 'm'})
        |WHERE 1 <> 2
        |RETURN n, r, m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.select("n").values("p").is(P.isEq("n")))
      .removes(__.select("r").values("p").is(P.isEq("r")))
      .removes(__.select("m").values("p").is(P.isEq("m")))
      .adds(__.hasLabel("N").has("p", P.isEq("n")))
      .adds(__.as("r").has("p", P.isEq("r")))
      .adds(__.hasLabel("M").has("p", P.isEq("m")))
  }

  @Test
  def multipleWhere(): Unit = {
    assertThat(parse("""
        |MATCH (n)-[r:R]->(m)
        |WHERE (n:N) AND n.p = 'n' AND (m:M) AND m.p = 'm' AND r.p = 'r'
        |RETURN n, r, m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.select("n").values("p").is(P.isEq("n")))
      .removes(__.select("r").values("p").is(P.isEq("r")))
      .removes(__.select("m").values("p").is(P.isEq("m")))
      .adds(__.hasLabel("N").has("p", P.isEq("n")))
      .adds(__.as("r").has("p", P.isEq("r")))
      .adds(__.hasLabel("M").has("p", P.isEq("m")))
  }

  @Test
  def multiplePaths(): Unit = {
    assertThat(parse("""
        |MATCH (n:N {p: 'n'})-[r1:R]->(m:M {p: 'm'})<-[r2:R]-(k)
        |MATCH (k:K {p: 'k'})
        |RETURN k
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.select("n").values("p").is(P.isEq("n")))
      .removes(__.select("k").values("p").is(P.isEq("k")))
      .removes(__.select("m").values("p").is(P.isEq("m")))
      .adds(__.as("n").hasLabel("N").has("p", P.isEq("n")))
      .adds(__.as("m").hasLabel("M").has("p", P.isEq("m")))
      .adds(__.as("k").hasLabel("K").has("p", P.isEq("k")))
  }

  @Test
  def variablePath(): Unit = {
    assertThat(parse("""
        |MATCH (n:N {p: 'n'})-[r*1..2]->(m)
        |RETURN m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(
          __.and(
            __.select("n").values("p").is(P.isEq("n")),
            __.select("n").hasLabel("N")
          )))
      .adds(__.as("n").hasLabel("N").has("p", P.isEq("n")))
  }

  @Test
  def merge(): Unit = {
    assertThat(parse("MERGE (n:N {p: 'n'})"))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.select("n").values("p").is(P.isEq("n")))
      .adds(__.as("n").has("p", P.isEq("n")))
  }

  @Test
  def unnamedVariables(): Unit = {
    assertThat(parse("""
        |MATCH (:person {name: 'marko'})-[r:knows]->(:person {name: 'josh'})
        |RETURN r
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .adds(__.V().as("  UNNAMED7").hasLabel("person").has("name", P.isEq("marko")))
      .adds(__.inV().as("  UNNAMED44").hasLabel("person").has("name", P.isEq("josh")))
  }
}
