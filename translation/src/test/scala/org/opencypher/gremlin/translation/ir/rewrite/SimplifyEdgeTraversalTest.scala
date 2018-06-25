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
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class SimplifyEdgeTraversalTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal,
      SimplifySingleProjections,
      RemoveUnusedAliases
    ),
    postConditions = Nil
  )

  @Test
  def outIn(): Unit = {
    assertThat(parse("""
        |MATCH ()-[r:R]->()
        |RETURN count(r) AS count
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifyEdgeTraversal)
      .removes(__.V().outE("R").as("r").inV().select("r"))
      .adds(__.E().as("r").hasLabel("R"))
  }

  @Test
  def inOut(): Unit = {
    assertThat(parse("""
        |MATCH ()<-[r:R]-()
        |RETURN count(r) AS count
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifyEdgeTraversal)
      .removes(__.V().inE("R").as("r").outV().select("r"))
      .adds(__.E().as("r").hasLabel("R"))
  }
}
