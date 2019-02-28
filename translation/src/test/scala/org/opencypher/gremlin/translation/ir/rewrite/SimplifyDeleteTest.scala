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

import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.Tokens
import org.opencypher.gremlin.translation.Tokens.{DELETE, DETACH_DELETE}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert._
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class SimplifyDeleteTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal,
      RemoveUselessSteps
    ),
    postConditions = Nil
  )

  @Test
  def delete(): Unit = {
    assertThat(parse("MATCH (n) DELETE n"))
      .withFlavor(flavor)
      .rewritingWith(SimplifyDelete)
      .removes(
        __.sideEffect(__.limit(0).aggregate(DELETE))
      )
      .removes(
        __.sideEffect(__.limit(0).aggregate(DETACH_DELETE))
      )
      .removes(
        __.cap(DETACH_DELETE)
          .unfold()
          .dedup()
          .is(P.neq(Tokens.NULL))
          .drop()
      )
      .keeps(
        __.cap(DELETE)
          .unfold()
          .dedup()
          .is(P.neq(Tokens.NULL))
      )
  }

  @Test
  def detachDelete(): Unit = {
    assertThat(parse("MATCH (n) DETACH DELETE n"))
      .withFlavor(flavor.extend(SimplifyDelete :: Nil))
      .contains(__.V().drop())
  }

  @Test
  def doesNotAffectChoose(): Unit = {
    assertThat(
      parse("MATCH (n) WITH collect(n) as typelost\n"
        + "DELETE typelost[$i]"))
      .withFlavor(flavor)
      .rewritingWith(SimplifyDelete)
      .keeps(
        __.sideEffect(__.limit(0).aggregate(DELETE))
          .sideEffect(__.limit(0).aggregate(DETACH_DELETE))
      )
      .keeps(
        __.cap(DELETE)
          .unfold()
          .dedup()
          .is(P.neq(Tokens.NULL))
      )
      .keeps(
        __.cap(DETACH_DELETE)
          .unfold()
          .dedup()
          .is(P.neq(Tokens.NULL))
          .drop()
      )
  }

}
