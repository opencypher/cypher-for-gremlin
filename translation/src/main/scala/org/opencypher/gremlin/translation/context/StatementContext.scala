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
package org.opencypher.gremlin.translation.context

import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.translator.Translator

import scala.collection.mutable

object StatementContext {
  def apply[T, P](dsl: Translator[T, P], extractedParameters: Map[String, Any]): StatementContext[T, P] = {
    new StatementContext(
      dsl,
      extractedParameters,
      new mutable.HashSet[String]
    )
  }
}

/**
  * Context used by AST walkers to share global translation state.
  *
  * @param dsl                   reference to [[Translator]] implementation in use
  * @param extractedParameters   Cypher query parameters
  * @param referencedAliases tracks node aliases referenced in translation
  */
sealed class StatementContext[T, P](
    val dsl: Translator[T, P],
    val extractedParameters: Map[String, Any],
    val referencedAliases: mutable.HashSet[String]) {

  private var midTraversals = 0

  def midTraversal(g: GremlinSteps[T, P]) {
    midTraversals += 1
    g.V()
  }

  def lowerBound(edges: Int): Int = if (edges == 0) 0 else edges * 2 + 1

  def upperBound(edges: Int): Int = lowerBound(edges) + midTraversals

  def unsupported[A](description: String, node: Any): A = {
    throw new UnsupportedOperationException(s"Unsupported $description: $node")
  }

  def precondition(expression: Boolean, message: String, node: Any) {
    if (!expression) {
      throw new UnsupportedOperationException(s"$message: $node")
    }
  }

  private var firstStatement = true

  def isFirstStatement: Boolean = {
    firstStatement
  }

  def markFirstStatement() {
    firstStatement = false
  }

  private var nameGenerator = new NameGenerator()

  def generateName(): String = {
    nameGenerator.next()
  }

  def copy(): StatementContext[T, P] = {
    val result = StatementContext(dsl, extractedParameters)
    result.referencedAliases ++= referencedAliases
    result.nameGenerator = nameGenerator
    result
  }
}
