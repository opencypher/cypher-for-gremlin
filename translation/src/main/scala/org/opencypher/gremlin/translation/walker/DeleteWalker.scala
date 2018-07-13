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

import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.Exceptions.DELETE_CONNECTED_NODE
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.util.symbols.{AnyType, NodeType, PathType, RelationshipType}

object DeleteWalker {
  def walkClause[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Delete): Unit = {
    new DeleteWalker(context, g).walkClause(node)
  }
}

class DeleteWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Delete): Unit = {
    val Delete(expressions, detach) = node

    val sideEffect = g.start()
    expressions.foreach(safeDelete(sideEffect, _, detach))

    g.barrier().sideEffect(sideEffect)
  }

  private def safeDelete(subTraversal: GremlinSteps[T, P], expr: Expression, checkBeforeDelete: Boolean) = {
    val p = context.dsl.predicates()
    val expressionTraversal = ExpressionWalker.walkLocal(context, g, expr)
    val typ = context.expressionTypes.get(expr)

    if (!checkBeforeDelete) {
      typ match {
        case Some(_: NodeType) =>
          expressionTraversal.sideEffect(
            g.start()
              .is(p.neq(Tokens.NULL))
              .bothE()
              .constant(DELETE_CONNECTED_NODE.toString)
              .map(CustomFunction.cypherException())
          )
        case Some(_: RelationshipType) =>
        case _                         =>
      }
    }

    typ match {
      case Some(_: PathType) =>
        expressionTraversal
          .unfold()
          .unfold()
      case Some(_: AnyType) => expressionTraversal.unfold()
      case _                =>
    }

    subTraversal.sideEffect(expressionTraversal.is(p.neq(Tokens.NULL)).drop())
  }
}
