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

import org.apache.tinkerpop.gremlin.structure.Column
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.Tokens
import org.opencypher.gremlin.translation.ir.builder.IRGremlinPredicates
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.{Translator, TranslatorFlavor}
import org.opencypher.gremlin.traversal.CustomFunction

class CustomFunctionsFallbackTest {
  private val flavor = TranslatorFlavor.gremlinServer
  private val P = new IRGremlinPredicates

  @Test
  def enableCypherExtensions(): Unit = {
    val translator = Translator.builder.traversal().enableCypherExtensions.build()

    assertThat(parse("MATCH (a) DELETE a"))
      .withFlavor(translator.flavor())
      .contains(__.map(CustomFunction.cypherException()))
  }

  @Test
  def disableCypherExtensions(): Unit = {
    val translator = Translator.builder.gremlinGroovy().build()

    assertThat(parse("MATCH (a) DELETE a"))
      .withFlavor(translator.flavor())
      .contains(
        __.bothE()
          .path()
          .from("Cannot delete node, because it still has relationships. To delete this node, you must first delete its relationships."))
  }

  @Test
  def cypherPlusFallback(): Unit = {
    assertThat(parse("RETURN 1 + $noType AS a"))
      .withFlavor(flavor)
      .rewritingWith(CustomFunctionFallback)
      .removes(__.select(Column.values).map(CustomFunction.cypherPlus()))
      .adds(
        __.select(Column.values)
          .local(__.unfold().choose(P.neq(Tokens.NULL), __.sum(), __.start().constant(Tokens.NULL))))
  }

}
