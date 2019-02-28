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
package org.opencypher.gremlin.translation.ir

import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.single
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions
import org.opencypher.gremlin.translation.translator.TranslatorFlavor

class CardinalityTest {

  @Test
  def createVertexProperties(): Unit = {
    CypherAstAssertions
      .assertThat(parse("CREATE (n {a: 'b', a: 'd'}) RETURN n.a"))
      .withFlavor(TranslatorFlavor.gremlinServer)
      .contains(__.property(single, "a", "b"))
      .contains(__.property(single, "a", "d"))
      .doesNotContain(__.property("a", "b"))
      .doesNotContain(__.property("a", "d"))
  }

  @Test
  def updateVertexProperties(): Unit = {
    CypherAstAssertions
      .assertThat(parse("MATCH (n:a) SET n.test = 1"))
      .withFlavor(TranslatorFlavor.gremlinServer)
      .contains(__.property(single, "test", 1L))
      .doesNotContain(__.property("test", 1L))
  }

  @Test
  def createEdgeProperties(): Unit = {
    CypherAstAssertions
      .assertThat(parse("CREATE ()-[r:ok {a: 'b', a: 'd'}]->() RETURN r.a"))
      .withFlavor(TranslatorFlavor.gremlinServer)
      .contains(__.property("a", "b"))
      .contains(__.property("a", "d"))
      .doesNotContain(__.property(single, "a", "b"))
      .doesNotContain(__.property(single, "a", "d"))
  }

  @Test
  def updateEdgeProperties(): Unit = {
    CypherAstAssertions
      .assertThat(parse("MATCH ()-[r:b]->() SET r.test = 1"))
      .withFlavor(TranslatorFlavor.gremlinServer)
      .contains(__.property("test", 1L))
      .doesNotContain(__.property(single, "test", 1L))
  }
}
