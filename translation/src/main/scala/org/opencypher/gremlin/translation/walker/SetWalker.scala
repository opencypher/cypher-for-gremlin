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

import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils.notNull
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition

/**
  * AST walker that handles translation
  * of the `SET` clause nodes in the Cypher AST.
  */
object SetWalker {
  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Clause): Unit = {
    new SetWalker(context, g).walkClause(node)
  }
}

private class SetWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Clause): Unit = {
    node match {
      case SetClause(items) =>
        walkSetClause(items)
      case Remove(items) =>
        walkRemoveClause(items)
      case _ =>
        context.unsupported("match clause", node)
    }

  }

  private def walkSetClause(items: Seq[SetItem]): Unit = {
    val p = context.dsl.predicates()
    items.foreach {
      case SetPropertyItem(Property(Variable(variable), PropertyKeyName(key)), expression: Expression) =>
        setProperty(variable, key, expression)
      case SetIncludingPropertiesFromMapItem(Variable(variable), MapExpression(pairs)) =>
        pairs.foreach {
          case (PropertyKeyName(key), expression) => setProperty(variable, key, expression)
        }
      case SetExactPropertiesFromMapItem(Variable(variable), MapExpression(pairs)) =>
        g.select(variable).sideEffect(g.start().is(p.neq(NULL)).properties().drop())
        pairs.foreach {
          case (PropertyKeyName(key), expression) => setProperty(variable, key, expression)
        }
      case n =>
        context.unsupported("set clause", n)
    }
  }

  private def walkRemoveClause(items: Seq[RemoveItem]): Unit = {
    items.foreach {
      case RemovePropertyItem(Property(Variable(variable), PropertyKeyName(key))) =>
        setProperty(variable, key, Null()(InputPosition.NONE))
      case n =>
        context.unsupported("set clause", n)
    }
  }

  private def setProperty(variable: String, key: String, value: Expression): Unit = {
    val traversal = ExpressionWalker.walkProperty(context, g.start(), key, value)
    g.select(variable).map(notNull(traversal, context))
  }
}
