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

import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.neo4j.cypher.internal.frontend.v3_3.ast._
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils._

/**
  * AST walker that handles translation
  * of the `WITH` clause nodes in the Cypher AST.
  */
object WithWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: With) {
    new WithWalker(context, g).walkClause(node)
  }
}

private class WithWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: With) {
    val With(_, ReturnItems(_, items), _, orderByOption, skip, limit, _) = node
    for (item <- items) {
      val AliasedReturnItem(expression, Variable(alias)) = item
      expression match {
        case Property(Variable(varName), PropertyKeyName(keyName)) =>
          g.select(varName).values(keyName).as(alias)
        case Variable(varName) =>
          if (varName != alias) {
            context.alias(alias)
            g.select(varName).as(alias)
          }
        case Parameter(name, _) =>
          g.constant(context.parameter(name)).as(alias)
        case l: Literal =>
          g.constant(inlineExpressionValue(l, context)).as(alias)
        case expression: Expression =>
          WhereWalker.walk(context, g, expression)
      }
    }

    if (orderByOption.isDefined) {
      sort(node)
    }

    for (s <- skip) {
      val Skip(expression) = s
      val value = inlineExpressionValue(expression, context, classOf[Number]).longValue()
      if (value != 0L) {
        g.skip(value)
      }
    }

    for (l <- limit) {
      val Limit(expression) = l
      val value = inlineExpressionValue(expression, context, classOf[Number]).longValue()
      g.limit(value)
    }
  }

  private def sort(node: Clause) {
    val With(_, ReturnItems(_, items), _, Some(OrderBy(sortItems)), _, _, _) = node
    val aliases = items.map(_.asInstanceOf[AliasedReturnItem]).map(_.name)
    g.select(aliases: _*).order()
    for (sortItem <- sortItems) {
      val order = sortItem match {
        case _: AscSortItem =>
          Order.incr
        case _: DescSortItem =>
          Order.decr
      }
      sortItem.expression match {
        case Variable(varName) =>
          g.by(g.start().select(varName), order)
        case _ =>
          context.unsupported("sort expression", sortItem.expression)
      }
    }
  }
}
