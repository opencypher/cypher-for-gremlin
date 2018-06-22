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

import java.util.Collections.{emptyList, emptyMap}

import org.apache.tinkerpop.gremlin.structure.Column
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.{P, __}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class SimplifyPropertySettersTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal
    ),
    postConditions = Nil
  )

  @Test
  def createProperties(): Unit = {
    assertThat(parse("CREATE ({foo: 'bar', baz: null, quux: $x})"))
      .withFlavor(flavor)
      .rewritingWith(SimplifyPropertySetters)
      .removes(__.property("foo", __.constant("bar")))
      .adds(__.property("foo", "bar"))
  }

  @Test
  def setProperties(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |SET
        |  n.p1 = [],
        |  n.p2 = [1],
        |  n.p3 = {},
        |  n.p4 = {k:1}
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(SimplifyPropertySetters)
      .removes(
        __.choose(
          __.constant(emptyList()).is(P.neq(NULL)).unfold,
          __.property("p1", __.constant(emptyList())),
          __.sideEffect(__.properties("p1").drop())
        ))
      .keeps(
        __.sideEffect(__.properties("p1").drop())
      )
      .keeps(
        __.property(
          "p2",
          __.project("  GENERATED1")
            .by(__.constant(1))
            .select(Column.values)
        )
      )
      .adds(
        __.property("p3", emptyMap())
      )
      .keeps(
        __.property(
          "p4",
          __.project("k").by(__.constant(1))
        )
      )
  }
}
