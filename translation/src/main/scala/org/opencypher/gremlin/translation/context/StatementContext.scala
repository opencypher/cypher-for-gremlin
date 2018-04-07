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

import org.neo4j.cypher.internal.frontend.v3_3.symbols.CypherType
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.translator.Translator

import scala.collection.mutable

object StatementContext {
  def apply[T, P](
      dsl: Translator[T, P],
      returnTypes: Map[String, CypherType],
      extractedParameters: Map[String, Any]): StatementContext[T, P] = {
    new StatementContext(dsl, returnTypes, extractedParameters)
  }
}

/**
  * Context used by AST walkers to share global translation state.
  *
  * @param dsl                 reference to [[Translator]] implementation in use
  * @param returnTypes         return types by alias
  * @param extractedParameters Cypher query parameters
  */
sealed class StatementContext[T, P](
    val dsl: Translator[T, P],
    val returnTypes: Map[String, CypherType],
    private val extractedParameters: Map[String, Any]) {

  def parameter(name: String): Object = {
    val value = extractedParameters.get(name).orNull
    val parameter = dsl.bindings().bind(name, value)
    parameter.asInstanceOf[Object]
  }

  def inlineParameter[R](name: String, klass: Class[R]): R = {
    val value = extractedParameters.get(name).orNull
    if (klass.isInstance(value)) {
      value.asInstanceOf[R]
    } else {
      unsupported("inlined parameter", value)
    }
  }

  private var midTraversals = 0

  def midTraversal(g: GremlinSteps[T, P]): Unit = {
    midTraversals += 1
    g.V()
  }

  def lowerBound(edges: Int): Int = if (edges == 0) 0 else edges * 2 + 1

  def upperBound(edges: Int): Int = lowerBound(edges) + midTraversals

  def unsupported(description: String, node: Any): Nothing = {
    throw new UnsupportedOperationException(s"Unsupported $description: $node")
  }

  def precondition(expression: Boolean, message: String, node: Any): Unit = {
    if (!expression) {
      throw new UnsupportedOperationException(s"$message: $node")
    }
  }

  private var firstStatement = true

  def isFirstStatement: Boolean = {
    firstStatement
  }

  def markFirstStatement(): Unit = {
    firstStatement = false
  }

  private val referencedAliases = mutable.HashSet.empty[String]

  /**
    * Returns a generated name if this alias already exists,
    * otherwise remembers the provided name.
    *
    * @param name original name
    * @return unique name
    */
  def alias(name: String): Option[String] = {
    if (referencedAliases.contains(name)) {
      val generated = generateName()
      Some(generated)
    } else {
      referencedAliases.add(name)
      None
    }
  }

  private var nameGenerator = new NameGenerator()

  def generateName(): String = {
    nameGenerator.next()
  }

  def copy(): StatementContext[T, P] = {
    val result = StatementContext(dsl, returnTypes, extractedParameters)
    result.referencedAliases ++= referencedAliases
    result.nameGenerator = nameGenerator
    result
  }
}
