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
import org.opencypher.gremlin.translation.Tokens.UNNAMED
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class RemoveUnusedAliasesTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal
    ),
    postConditions = Nil
  )

  @Test
  def generated(): Unit = {
    assertThat(parse("""
        |MATCH (n)-->()
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUnusedAliases)
      .removes(__.as("  cypher.path.start.GENERATED1"))
      .removes(__.as(UNNAMED + 10))
      .removes(__.as(UNNAMED + 13))
      .keeps(__.as("n"))
  }

  @Test
  def explicit(): Unit = {
    assertThat(parse("""
        |MATCH (n)-[r]->(m)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUnusedAliases)
      .removes(__.as("  cypher.path.start.GENERATED1"))
      .removes(__.as("r"))
      .removes(__.as("m"))
      .keeps(__.as("n"))
  }

  @Test
  def fromTo(): Unit = {
    assertThat(parse("CREATE (n)-[:R]->(m)"))
      .withFlavor(flavor)
      .rewritingWith(RemoveUnusedAliases)
      .removes(__.as(UNNAMED + 11))
      .keeps(__.as("n"))
      .keeps(__.as("m"))
  }

  @Test
  def reAlias(): Unit = {
    assertThat(parse("""
        |MATCH (n)-->(m)
        |MATCH (m)-->(k)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUnusedAliases)
      .removes(__.as("  cypher.path.start.GENERATED1"))
      .removes(__.as(UNNAMED + 10))
      .removes(__.as(UNNAMED + 26))
      .keeps(__.as("n"))
      .keeps(__.as("m"))
  }

  @Test
  def adjacentAs(): Unit = {
    assertThat(parse("""
         |MATCH ()-[r:R]->()
         |RETURN r
       """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveUnusedAliases)
      .removes(__.as("  cypher.path.start.GENERATED1"))
      .removes(__.as(UNNAMED + 7))
      .removes(__.as(UNNAMED + 17))
      .keeps(__.as("r"))
  }
}
