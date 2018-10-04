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

import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.CypherExceptions.DELETE_CONNECTED_NODE
import org.opencypher.gremlin.translation.walker.NodeUtils.runtimeValidation
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.{Expression, Variable}
import org.opencypher.v9_0.util.symbols.{AnyType, NodeType, PathType, RelationshipType}

object DeleteWalker {
  def walkClause[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Delete): Unit = {
    new DeleteWalker(context, g).walkClause(node)
  }

  def deleteAggregated[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]): Unit = {
    new DeleteWalker(context, g).dropAggregated()
  }
}

class DeleteWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {
  private def __ = g.start()

  def walkClause(node: Delete): Unit = {
    val Delete(expressions, detach) = node
    g.barrier()
    initEmptyCollection(g, DELETE)
    initEmptyCollection(g, DETACH_DELETE)
    expressions.foreach(aggregateForDrop(g, _, detach))
  }

  private def aggregateForDrop(
      subTraversal: GremlinSteps[T, P],
      expr: Expression,
      detach: Boolean): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    val expressionTraversal = ExpressionWalker.walkLocal(context, g, expr)
    val typ = context.expressionTypes.get(expr)

    typ match {
      case Some(_: NodeType) if !detach =>
        subTraversal.sideEffect(expressionTraversal.aggregate(DELETE))
      case Some(_: PathType) if expr.isInstanceOf[Variable] =>
        val deleteType = if (detach) DETACH_DELETE else DELETE
        val Variable(n) = expr
        subTraversal.sideEffect(
          expressionTraversal
            .unfold()
            .unfold()
            .where(p.without(PATH_EDGE + n))
            .aggregate(deleteType))
      case Some(_: RelationshipType) | Some(_: NodeType) =>
        subTraversal.sideEffect(expressionTraversal.aggregate(DETACH_DELETE))
      case Some(_: AnyType) if detach =>
        subTraversal.sideEffect(expressionTraversal.unfold().aggregate(DETACH_DELETE))
      case _ =>
        subTraversal.sideEffect(
          expressionTraversal.unfold().choose(p.isNode, __.aggregate(DELETE), __.aggregate(DETACH_DELETE)))
    }
  }

  def dropAggregated(): Unit = {
    val p = context.dsl.predicates()

    val delete = __
      .cap(DETACH_DELETE)
      .unfold()
      .dedup()
      .is(p.neq(Tokens.NULL))
      .drop()

    delete
      .cap(DELETE)
      .unfold()
      .dedup()
      .is(p.neq(Tokens.NULL))
      .flatMap(runtimeValidation(__.bothE(), DELETE_CONNECTED_NODE, context))
      .drop()

    sideEffectOnceForAllTraversers(g, delete)
  }

  def sideEffectOnceForAllTraversers(g: GremlinSteps[T, P], run: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    val runAndPut = __.constant(true).aggregate(DELETE_ONCE).flatMap(run)

    initEmptyCollection(g, DELETE_ONCE)

    g.barrier()
      .sideEffect(
        __.coalesce(
          __.cap(DELETE_ONCE).unfold(),
          runAndPut
        )
      )
  }

  def initEmptyCollection(g: GremlinSteps[T, P], name: String): Unit = {
    g.sideEffect(__.limit(0).aggregate(name))
  }
}
