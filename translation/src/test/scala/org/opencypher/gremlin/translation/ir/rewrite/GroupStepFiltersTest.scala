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

import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.single
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.Tokens
import org.opencypher.gremlin.translation.Tokens.{GENERATED, NULL, UNNAMED}
import org.opencypher.gremlin.translation.ir.builder.IRGremlinBindings
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.{P, __}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.ir.model.GremlinBinding
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

import scala.collection.JavaConverters._

class GroupStepFiltersTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal
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
  def multipleLabels(): Unit = {
    assertThat(parse("""
        |MATCH (n:A:B)
        |RETURN 1
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.where(__.select("n").hasLabel("A").hasLabel("B")))
      .keeps(__.hasLabel("A"))
      .keeps(__.hasLabel("B"))
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
  def whereId(): Unit = {
    assertThat(parse("""
                       |MATCH (n)
                       |WHERE id(n) = 1
                       |RETURN n
                     """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(__.select("n").choose(P.neq(Tokens.NULL), __.id()).is(P.neq(Tokens.NULL)).is(P.isEq(1)))
      )
      .adds(
        __.has(T.id.getAccessor, P.isEq(1))
      )
  }

  @Test
  def whereIdIn(): Unit = {
    assertThat(parse("""
                       |MATCH (n)
                       |WHERE id(n) in [1]
                       |RETURN n
                     """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(
          __.select("n")
            .choose(P.neq(NULL), __.id())
            .is(P.neq(NULL))
            .is(P.within(1.asInstanceOf[AnyRef])))
      )
      .adds(__.has(T.id.getAccessor, P.within(1.asInstanceOf[AnyRef])))
  }

  @Test
  def whereIdInMultiple(): Unit = {
    assertThat(parse("""
                       |MATCH (n)
                       |WHERE id(n) in [1, 2]
                       |RETURN n
                     """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(
          __.select("n")
            .choose(P.neq(NULL), __.id())
            .is(P.neq(NULL))
            .is(P.within(1.asInstanceOf[AnyRef], 2.asInstanceOf[AnyRef]))
        ))
      .adds(__.has(T.id.getAccessor, P.within(1.asInstanceOf[AnyRef], 2.asInstanceOf[AnyRef])))
  }

  @Test
  def whereWithParam(): Unit = {
    val params = new java.util.HashMap[String, Any](Map("nID" -> 1).asJava)
    assertThat(
      parse(
        """
          |MATCH (n)
          |WHERE id(n) = {nID}
          |RETURN n
        """.stripMargin,
        params
      ))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(
          __.choose(__.constant(GremlinBinding("nID")), __.constant(GremlinBinding("nID")), __.constant(NULL))
            .is(P.neq(NULL))
            .as(GENERATED + 1)
            .select("n")
            .choose(P.neq(NULL), __.id())
            .is(P.neq(NULL))
            .where(P.isEq(GENERATED + 1))))
      .adds(__.has(T.id.getAccessor, P.isEq(GremlinBinding("nID"))))
  }

  @Test
  def whereWithParams(): Unit = {
    val params = new java.util.HashMap[String, Any](Map("nID" -> 1).asJava)
    assertThat(
      parse(
        """
                       |MATCH (n)
                       |WHERE id(n) IN [{nID}]
                       |RETURN n
                     """.stripMargin,
        params
      ))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(
        __.where(
          __.select("n")
            .choose(P.neq(NULL), __.id())
            .is(P.neq(NULL))
            .is(P.within(GremlinBinding("nID"))))
      )
      .adds(__.has(T.id.getAccessor, P.within(GremlinBinding("nID"))))
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
      .adds(__.V().as(UNNAMED + 7).hasLabel("person").has("name", P.isEq("marko")))
      .adds(__.inV().as(UNNAMED + 44).hasLabel("person").has("name", P.isEq("josh")))
  }

  @Test
  def keepAdditions(): Unit = {
    assertThat(parse("MERGE p = (a {x: 1}) RETURN p"))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .keeps(__.addV().as("a").property(single, "x", __.constant(1)))
  }

  @Test
  def collectionOfParameters(): Unit = {
    val ids = new IRGremlinBindings().bind("ids", 1)

    assertThat(parse("MATCH (p:Person) WHERE id(p) in {ids} RETURN p.name"))
      .withFlavor(flavor)
      .rewritingWith(GroupStepFilters)
      .removes(__.choose(__.constant(ids), __.constant(ids), __.constant(NULL)))
      .removes(__.where(P.within(GENERATED + "1")))
      .adds(__.has("~id", P.within(ids)))
      .debug()
  }
}
