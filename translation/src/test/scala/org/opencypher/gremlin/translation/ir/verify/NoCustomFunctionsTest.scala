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
package org.opencypher.gremlin.translation.ir.verify

import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst
import org.opencypher.gremlin.translation.ir.helpers.JavaHelpers.assertThatThrownBy
import org.opencypher.gremlin.translation.translator.{Translator, TranslatorFlavor}

class NoCustomFunctionsTest {

  val flavor = new TranslatorFlavor(
    rewriters = Nil,
    postConditions = Seq(
      NoCustomFunctions
    )
  )

  @Test
  def functionsAndPredicates(): Unit = {
    val ast = CypherAst.parse("""
        |MATCH (n:N)
        |WITH n.p AS s
        |WHERE s STARTS WITH 'x' AND s ENDS WITH 'x' AND s CONTAINS 'x'
        |RETURN toString(s)
    """.stripMargin)
    val translator = Translator.builder.gremlinGroovy.build(flavor)

    assertThatThrownBy(() => ast.buildTranslation(translator))
      .hasMessageContaining("cypherContains, cypherEndsWith, cypherStarsWith, cypherToString")
  }
}
