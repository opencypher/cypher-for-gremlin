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
package org.opencypher.gremlin.translation.walker

import java.util

import org.neo4j.cypher.internal.frontend.v3_2.ast._
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils.{expressionValue, setProperty}

/**
  * AST walker that handles translation
  * of the `SET` clause nodes in the Cypher AST.
  */
object SetWalker {
  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Clause) {
    new SetWalker(context, g).walkClause(node)
  }
}

private class SetWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Clause) {
    node match {
      case SetClause(items) =>
        walkSetClause(items)
      case Remove(items) =>
        walkRemoveClause(items)
      case _ =>
        context.unsupported("match clause", node)
    }

  }

  private def walkSetClause(items: Seq[SetItem]) {
    items.foreach {
      case SetPropertyItem(Property(Variable(variable), PropertyKeyName(key)), expression: Expression) =>
        val value = expressionValue(expression, context)
        if (value.isInstanceOf[util.Collection[_]]) {
          applySideEffect(variable, setProperty(_, key, null))
        }
        applySideEffect(variable, setProperty(_, key, value))
      case SetIncludingPropertiesFromMapItem(Variable(variable), MapExpression(pairs)) =>
        applySideEffect(variable, setProperties(_, pairs))
      case SetExactPropertiesFromMapItem(Variable(variable), MapExpression(pairs)) =>
        applySideEffect(variable, _.properties().drop())
        applySideEffect(variable, setProperties(_, pairs))
      case n =>
        context.unsupported("set clause", n)
    }
  }

  private def walkRemoveClause(items: Seq[RemoveItem]) {
    items.foreach {
      case RemovePropertyItem(Property(Variable(variable), PropertyKeyName(key))) =>
        applySideEffect(variable, _.properties(key).drop())
      case n =>
        context.unsupported("set clause", n)
    }
  }

  private def applySideEffect(variable: String, setter: GremlinSteps[T, P] => Unit) {
    val p = context.dsl.predicates()
    val sideEffect = g.start().select(variable)
    setter(sideEffect)
    g.choose(p.neq(NULL), g.start().sideEffect(sideEffect))
  }

  private def setProperties(g: GremlinSteps[T, P], items: Seq[(PropertyKeyName, Expression)]) {
    for ((PropertyKeyName(key), expression) <- items) {
      val value = expressionValue(expression, context)
      setProperty(g, key, value)
    }
  }
}
