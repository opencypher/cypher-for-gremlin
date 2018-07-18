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
import org.opencypher.gremlin.translation.Tokens
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.ir.builder.IRGremlinPredicates
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class RemoveMultipleAliasesTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal
    ),
    postConditions = Nil
  )

  @Test
  def keepUnused(): Unit = {
    assertThat(parse("MATCH (n) RETURN (n:Person) AS isPerson, count(*) AS count"))
      .withFlavor(flavor)
      .rewritingWith(RemoveMultipleAliases)
      .keeps(__.as("n").as(UNUSED))
  }

  @Test
  def keepShadowedAliases(): Unit = {
    assertThat(parse("""MATCH (p:person)
        |OPTIONAL MATCH (p)-[c:created]->(s:software)
        |RETURN s AS soft
      """.stripMargin))
      .withFlavor(flavor)
      .keeps(__.as("c").as("s"))
  }

  @Test
  def rewritePath(): Unit = {
    assertThat(parse("""MATCH (o:Officer)
        |WHERE o.name = 'PELAGOS YACHTS LIMITED'
        |MATCH path = (o)-[r]->(:Entity)
        |RETURN path LIMIT 100""".stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveMultipleAliases)
      .removes(__.as("  cypher.match.start.path").as("  GENERATED1"))
      .removes(__.as(UNNAMED + 81).as("  cypher.match.end.path"))
      .keeps(__.as("o"))
      .keeps(__.as("  cypher.match.end.path"))
  }

  @Test
  def rewriteVarLengthPath(): Unit = {
    assertThat(parse("""MATCH p = (n {name: 'A'})-[:KNOWS*1..2]->(x)
        |RETURN p""".stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveMultipleAliases)
      .removes(
        __.as("  cypher.match.start.p")
          .as("n")
          .as("  cypher.path.start.p"))
      .keeps(__.as("n"))
      .removes(__.from("  cypher.path.start.p"))
      .adds(__.from("n"))
      .removes(__.as("x").as("  cypher.match.end.p"))
      .keeps(__.as("x"))
  }

  @Test
  def replace(): Unit = {
    val P = new IRGremlinPredicates

    val steps = __
      .V()
      .as("l1")
      .as("l2")
      .outE()
      .as("other")
      .path()
      .from("l2")
      .to("l2")
      .dedup("l2")
      .dedup("l2", "other")
      .math("1 + l2")
      .where(P.isEq("l2"))
      .select("l2", "other")
      .select("l2")

    val expected = __
      .V()
      .as("l1")
      .outE()
      .as("other")
      .path()
      .from("l1")
      .to("l1")
      .dedup("l1")
      .dedup("l1", "other")
      .math("1 + l1")
      .where(P.isEq("l1"))
      .select("l1", "other")
      .select("l1")

    val rewritten = RemoveMultipleAliases.apply(steps.current())

    assertThat(rewritten)
      .isEqualTo(expected.current())
  }

  @Test
  def replaceMany(): Unit = {
    val steps = __
      .V()
      .as("l1")
      .as("l2")
      .as("l3")
      .as("l4")
      .as("l5")
      .select("l1")
      .select("l2")
      .select("l3")
      .select("l4")
      .select("l5")

    val expected = __
      .V()
      .as("l1")
      .select("l1")
      .select("l1")
      .select("l1")
      .select("l1")
      .select("l1")

    val rewritten = RemoveMultipleAliases.apply(steps.current())

    assertThat(rewritten)
      .isEqualTo(expected.current())
  }

  @Test
  def replaceEnd(): Unit = {
    val steps = __
      .V()
      .as("l1")
      .as("l2")

    val expected = __
      .V()
      .as("l1")

    val rewritten = RemoveMultipleAliases.apply(steps.current())

    assertThat(rewritten)
      .isEqualTo(expected.current())
  }

  @Test
  def replaceWithin(): Unit = {
    val P = new IRGremlinPredicates

    val steps = __
      .V()
      .as("l1")
      .as("l2")
      .where(P.within("l2"))

    val expected = __
      .V()
      .as("l1")
      .where(P.within("l1"))

    val rewritten = RemoveMultipleAliases.apply(steps.current())

    assertThat(rewritten)
      .isEqualTo(expected.current())
  }

  @Test
  def pickUserDefined(): Unit = {
    val steps = __
      .V()
      .as(Tokens.PATH_START)
      .as(Tokens.MATCH_START)
      .as("  GENERATED1")
      .as("n")
      .as("  GENERATED2")
      .select(Tokens.PATH_START)
      .select(Tokens.MATCH_START)
      .select("n")
      .select("  GENERATED1")
      .select("  GENERATED2")

    val expected = __
      .V()
      .as("n")
      .select("n")
      .select("n")
      .select("n")
      .select("n")
      .select("n")

    val rewritten = RemoveMultipleAliases.apply(steps.current())

    assertThat(rewritten)
      .isEqualTo(expected.current())
  }
}
