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
import org.opencypher.gremlin.translation.Tokens.UNUSED
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class SimplifySingleProjectionsTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal
    ),
    postConditions = Nil
  )

  @Test
  def pivotAndAggregation(): Unit = {
    assertThat(parse("""
        |MATCH (n:Person) RETURN
        |n.lastName,collect(n.firstName)
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifySingleProjections)
      .removes(__.as(UNUSED))
      .removes(__.select("n", UNUSED))
  }

  @Test
  def aggregation(): Unit = {
    assertThat(parse("""
        |MATCH (n1)-[r]->(n2)
        |RETURN count(n1)
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifySingleProjections)
      .removes(__.as(UNUSED))
      .removes(__.select("n1", UNUSED))
  }

  @Test
  def pivot(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifySingleProjections)
      .removes(__.as(UNUSED))
      .removes(__.select("n", UNUSED))
  }

}
