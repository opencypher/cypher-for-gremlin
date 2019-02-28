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
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert._
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class RemoveIdentityReselectTest {

  val flavor = new TranslatorFlavor(
    rewriters = Seq(
      InlineFlatMapTraversal,
      GroupStepFilters
    ),
    postConditions = Nil
  )

  @Test
  def aliasProjection(): Unit = {
    assertThat(parse("""
        |MATCH (n)
        |UNWIND labels(n) as l
        |RETURN l
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveIdentityReselect)
      .removes(__.as("n").select("n"))
      .keeps(__.as("n"))
  }

  @Test
  def aliasProjectionWithHas(): Unit = {
    assertThat(parse("""
        |MATCH (n:L {foo: 'bar'})
        |UNWIND labels(n) as l
        |RETURN l
      """.stripMargin))
      .withFlavor(flavor)
      .rewritingWith(RemoveIdentityReselect)
      .removes(__.as("n").hasLabel("L").has("foo", P.isEq("bar")).select("n"))
      .keeps(__.as("n").hasLabel("L").has("foo", P.isEq("bar")))
  }

}
