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

import org.opencypher.gremlin.translation.translator.Translator
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.util.symbols.CypherType

import scala.collection.mutable

object StatementContext {
  def apply[T, P](
      dsl: Translator[T, P],
      expressionTypes: Map[Expression, CypherType],
      returnTypes: Map[String, CypherType],
      parameters: Map[String, Any]): StatementContext[T, P] = {
    new StatementContext(dsl, expressionTypes, returnTypes, parameters)
  }
}

/**
  * Context used by AST walkers to share global translation state.
  *
  * @param dsl             reference to [[Translator]] implementation in use
  * @param expressionTypes expression Cypher types
  * @param returnTypes     return types by alias
  * @param parameters      Cypher query parameters
  */
sealed class StatementContext[T, P](
    val dsl: Translator[T, P],
    val expressionTypes: Map[Expression, CypherType],
    val returnTypes: Map[String, CypherType],
    private val parameters: Map[String, Any]) {

  def parameter(name: String): Object = {
    val value = parameters.get(name).orNull
    val parameter = dsl.bindings().bind(name, value)
    parameter.asInstanceOf[Object]
  }

  def inlineParameter[R](name: String, klass: Class[R]): R = {
    val value = parameters.get(name).orNull
    if (klass.isInstance(value)) {
      value.asInstanceOf[R]
    } else {
      unsupported("inlined parameter", value)
    }
  }

  def parameterDefined(name: String): Boolean = {
    parameters.contains(name)
  }

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
    val result = StatementContext(dsl, expressionTypes, returnTypes, parameters)
    result.referencedAliases ++= referencedAliases
    result.nameGenerator = nameGenerator
    result
  }
}
