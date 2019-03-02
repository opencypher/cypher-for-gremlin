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

import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert._
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class RemoveUselessNullChecksTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal,
      RemoveIntermediateProjection,
      SimplifySingleProjections
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
      .rewritingWith(RemoveUselessNullChecks)
      .removes(__.by(__.choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .adds(__.by(__.valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def multipleProjections(): Unit = {
    assertThat(parse("""
        |MATCH (n)-->(m)
        |RETURN n, m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUselessNullChecks)
      .removes(__.by(__.select("n").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .removes(__.by(__.select("m").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .adds(__.by(__.select("n").valueMap().`with`(WithOptions.tokens)))
      .adds(__.by(__.select("m").valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def singleOptionalProjection(): Unit = {
    assertThat(parse("""
        |OPTIONAL MATCH (n)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUselessNullChecks)
      .keeps(__.choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def functionInvocation(): Unit = {
    assertThat(parse("""
      MATCH (n:notExising) WITH n AS n RETURN head(collect(n)) AS head
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUselessNullChecks)
      .keeps(__.choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def multipleOptionalProjections(): Unit = {
    assertThat(parse("""
        |OPTIONAL MATCH (n)-->(m)
        |RETURN n, m
      """.stripMargin))
      .withFlavor(flavor)
      .keeps(__.select("n").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens)))
      .keeps(__.select("m").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def optionalWithProjection(): Unit = {
    assertThat(parse("""
        |OPTIONAL MATCH (n:notExisting) WITH (n) as m RETURN m
      """.stripMargin))
      .withFlavor(flavor)
      .contains(__.choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def create(): Unit = {
    assertThat(parse("""
        |CREATE (n)-[r:knows]->(m) RETURN n, r, m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUselessNullChecks)
      .removes(__.by(__.select("n").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .removes(
        __.by(
          __.select("r")
            .choose(
              P.neq("  cypher.null"),
              __.project("  cypher.element", "  cypher.inv", "  cypher.outv")
                .by(__.valueMap().`with`(WithOptions.tokens))
                .by(__.inV().id())
                .by(__.outV().id())
            )))
      .removes(__.by(__.select("m").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .adds(__.by(__.select("n").valueMap().`with`(WithOptions.tokens)))
      .adds(
        __.by(
          __.select("r")
            .project("  cypher.element", "  cypher.inv", "  cypher.outv")
            .by(__.valueMap().`with`(WithOptions.tokens))
            .by(__.inV().id())
            .by(__.outV().id())))
      .adds(__.by(__.select("m").valueMap().`with`(WithOptions.tokens)))
  }

  @Test
  def merge(): Unit = {
    assertThat(parse("""
        |MERGE (n)-[r:knows]->(m) RETURN n, r, m
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUselessNullChecks)
      .removes(__.by(__.select("n").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .removes(
        __.by(
          __.select("r")
            .choose(
              P.neq("  cypher.null"),
              __.project("  cypher.element", "  cypher.inv", "  cypher.outv")
                .by(__.valueMap().`with`(WithOptions.tokens))
                .by(__.inV().id())
                .by(__.outV().id())
            )))
      .removes(__.by(__.select("m").choose(P.neq(NULL), __.valueMap().`with`(WithOptions.tokens))))
      .adds(__.by(__.select("n").valueMap().`with`(WithOptions.tokens)))
      .adds(
        __.by(
          __.select("r")
            .project("  cypher.element", "  cypher.inv", "  cypher.outv")
            .by(__.valueMap().`with`(WithOptions.tokens))
            .by(__.inV().id())
            .by(__.outV().id())))
      .adds(__.by(__.select("m").valueMap().`with`(WithOptions.tokens)))
  }
}
