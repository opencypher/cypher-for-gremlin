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

class InlineFlatMapTraversalTest {

  @Test
  def inlineProjectionFlatMap(): Unit = {
    val projection = __.project("n").by(__.constant(1))

    assertThat(parse("""
        |WITH 1 AS n
        |RETURN n
      """.stripMargin))
      .withFlavor(TranslatorFlavor.empty)
      .rewritingWith(InlineFlatMapTraversal)
      .removes(__.flatMap(projection))
      .keeps(projection)
  }

  @Test
  def adjacentFlatMap(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |WHERE (n)-->(:L)
        |RETURN n
      """.stripMargin))
      .withFlavor(TranslatorFlavor.empty)
      .rewritingWith(InlineFlatMapTraversal)
      .removes(__.flatMap(__))
      .removes(__.as("  cypher.path.start.GENERATED2").flatMap(__.outE().inV()))
      .adds(__.as("  cypher.path.start.GENERATED2").outE().inV())
  }
}
