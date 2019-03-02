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
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.ir.builder.IRGremlinPredicates
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor
import org.opencypher.gremlin.translation.traversal.DeprecatedOrderAccessor.{decr, incr}

class Gremlin33xFlavorTest {

  private val flavor = TranslatorFlavor.gremlinServer

  private val P = new IRGremlinPredicates

  @Test
  def values(): Unit = {
    assertThat(parse("""
        |MATCH (n:N)
        |RETURN n
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(Gremlin33xFlavor)
      .removes(__.valueMap().`with`(WithOptions.tokens))
      .adds(__.valueMap(true))
  }
  @Test
  def incrInOrder(): Unit = {
    assertThat(parse("MATCH (n) RETURN n ORDER BY n.name"))
      .withFlavor(flavor)
      .rewritingWith(Gremlin33xFlavor)
      .adds(
        __.by(
          __.select("n")
            .choose(P.neq(NULL), __.choose(__.values("name"), __.values("name"), __.constant(NULL))),
          incr))
  }

  @Test
  def decrInOrder(): Unit = {
    assertThat(parse("MATCH (n) RETURN n ORDER BY n.name DESC"))
      .withFlavor(flavor)
      .rewritingWith(Gremlin33xFlavor)
      .adds(
        __.by(
          __.select("n")
            .choose(P.neq(NULL), __.choose(__.values("name"), __.values("name"), __.constant(NULL))),
          decr))
  }

}
