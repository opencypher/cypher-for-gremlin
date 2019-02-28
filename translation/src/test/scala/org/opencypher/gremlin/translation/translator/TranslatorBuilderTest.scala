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
package org.opencypher.gremlin.translation.translator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.opencypher.gremlin.translation.CypherAst.parse
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.ir.builder.{IRGremlinBindings, IRGremlinPredicates, IRGremlinSteps}
import org.opencypher.gremlin.translation.ir.helpers.CypherAstAssert.__
import org.opencypher.gremlin.translation.ir.helpers.JavaHelpers.assertThatThrownBy
import org.opencypher.gremlin.translation.ir.helpers.TraversalAssertions
import org.opencypher.gremlin.translation.ir.model.{GremlinPredicate, GremlinStep}
import org.opencypher.gremlin.translation.ir.rewrite.{CosmosDbFlavor, CustomFunctionFallback, NeptuneFlavor}
import org.opencypher.gremlin.traversal.CustomFunction

import scala.collection.JavaConverters._

class TranslatorBuilderTest {
  @Test
  def cosmosDb(): Unit = {
    val dslBuilder = createBuilder.build("cosmosdb")

    assertThatThrownBy(
      () =>
        parse("RETURN toupper('test')")
          .buildTranslation(dslBuilder))
      .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper")

    val steps = parse("MATCH (n) RETURN n.name")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CosmosDbFlavor)).isTrue
    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
    assertContains(steps, __.properties().hasKey("name"))
    assertNotContains(steps, __.values("name"))
  }

  @Test
  def cosmosDbExtensions(): Unit = {
    val dslBuilder = createBuilder.build("cosmosdb+cfog_server_extensions")

    val steps = parse("MATCH (n) RETURN toupper(n.name)")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CosmosDbFlavor)).isTrue
    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
    assertContains(steps, __.map(CustomFunction.cypherToUpper()))
    assertContains(steps, __.properties().hasKey("name"))
    assertNotContains(steps, __.values("name"))
  }

  @Test
  def neptuneDb(): Unit = {
    val dslBuilder = createBuilder.build("neptune")

    assertThatThrownBy(
      () =>
        parse("RETURN toupper('test')")
          .buildTranslation(dslBuilder))
      .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper")

    val steps = parse("MATCH (n) WHERE n.age=$age RETURN count(n)", Map("age" -> 25).asJava)
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(NeptuneFlavor)).isTrue
    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
    assertContains(steps, __.constant(25)) // inline parameters
    assertContains(steps, __.count().barrier())
  }

  @Test
  def neptuneExtensions(): Unit = {
    val dslBuilder = createBuilder.build("neptune+cfog_server_extensions")

    val steps = parse("MATCH (n) WHERE toupper(n.age)=$age RETURN count(n)", Map("age" -> 25).asJava)
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(NeptuneFlavor)).isTrue
    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
    assertContains(steps, __.constant(25)) // inline parameters
    assertContains(steps, __.count().barrier())
  }

  @Test
  def gremlin(): Unit = {
    val dslBuilder = createBuilder.build("gremlin")

    assertThatThrownBy(
      () =>
        parse("RETURN toupper('test')")
          .buildTranslation(dslBuilder))
      .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper")

    val steps = parse("MATCH (n) RETURN n.name")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
  }

  @Test
  def vanilla(): Unit = {
    val dslBuilder = createBuilder.build("vanilla")

    assertThatThrownBy(
      () =>
        parse("RETURN toupper('test')")
          .buildTranslation(dslBuilder))
      .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper")

    val steps = parse("MATCH (n) RETURN n.name")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
  }

  @Test
  def empty(): Unit = {
    val dslBuilder = createBuilder.build("")

    assertThatThrownBy(
      () =>
        parse("RETURN toupper('test')")
          .buildTranslation(dslBuilder))
      .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper")

    val steps = parse("MATCH (n) RETURN n.name")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
  }

  @Test
  def nullParam(): Unit = {
    val dslBuilder = createBuilder.build("")

    assertThatThrownBy(
      () =>
        parse("RETURN toupper('test')")
          .buildTranslation(dslBuilder))
      .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper")

    val steps = parse("MATCH (n) RETURN n.name")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
  }

  @Test
  def gremlinExtensions(): Unit = {
    val dslBuilder = createBuilder.build("gremlin+cfog_server_extensions")

    val steps = parse("MATCH (n) RETURN toupper(n.name)")
      .buildTranslation(dslBuilder)

    assertThat(dslBuilder.flavor().rewriters.contains(CustomFunctionFallback)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.CYPHER_EXTENSIONS)).isTrue
    assertThat(dslBuilder.isEnabled(TranslatorFeature.MULTIPLE_LABELS)).isFalse
    assertThat(dslBuilder.isEnabled(TranslatorFeature.RETURN_GREMLIN_ELEMENTS)).isFalse
  }

  private def createBuilder = {
    Translator
      .builder()
      .custom(
        new IRGremlinSteps,
        new IRGremlinPredicates,
        new IRGremlinBindings
      )
  }

  private def assertContains(steps: Seq[GremlinStep], traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]) = {
    TraversalAssertions.traversalContains("Traversal", steps, traversal.current())
  }

  private def assertNotContains(
      steps: Seq[GremlinStep],
      traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]) = {
    TraversalAssertions.traversalNotContains("Traversal", steps, traversal.current())
  }
}
