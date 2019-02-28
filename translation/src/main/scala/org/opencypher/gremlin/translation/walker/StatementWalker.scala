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
package org.opencypher.gremlin.translation.walker

import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.EntityNotFound
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.{FunctionInvocation, FunctionName, Property}
import org.opencypher.v9_0.util.{ASTNode, InputPosition}

/**
  * AST walker that starts translation of the Cypher AST.
  */
object StatementWalker {
  def walk[T, P](context: WalkerContext[T, P], node: Statement): Unit = {
    val g = context.dsl.steps()
    new StatementWalker(context, g).walk(node)
  }
}

class StatementWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

  def walk(node: Statement): Unit = {
    node match {
      case Query(_, part) =>
        part match {
          case union: Union =>
            walkUnion(union)
          case single: SingleQuery =>
            walkSingle(single)
        }
    }
  }

  def walkUnion(node: Union): Unit = {
    ensureFirstStatement(g, context)

    val queries = flattenUnion(Vector(), node)
    val subGs = queries.map { query =>
      val subContext = context.copy()
      val subG = g.start()
      new StatementWalker(subContext, subG).walkSingle(query)
      subG
    }
    g.union(subGs: _*)

    val distinct = node.isInstanceOf[UnionDistinct]
    if (distinct) {
      g.dedup()
    }
  }

  private def flattenUnion(acc: Vector[SingleQuery], union: Union): Vector[SingleQuery] = {
    union.part match {
      case subUnion: Union =>
        flattenUnion(Vector(), subUnion) :+ union.query
      case query: SingleQuery =>
        acc :+ query :+ union.query
    }
  }

  def walkSingle(node: SingleQuery): Unit = {
    val clauses = node.clauses

    clauses match {
      case Seq(callClause: UnresolvedCall) =>
        CallWalker.walkStandalone(context, g, callClause)
      case _ =>
        rewriteClauses(clauses).foreach(walkClause)
    }
  }

  def rewriteClauses(clauses: Seq[Clause]): Seq[ASTNode] = {
    val isReturnDependsOnDelete = returnDependsOnDelete(clauses)
    val maybeEmptyReturn = if (!clauses.exists(_.isInstanceOf[Return])) Seq(EmptyReturn()) else Nil

    clauses.flatMap {
      case deleteClause: Delete if isReturnDependsOnDelete => Seq(deleteClause)
      case deleteClause: Delete                            => Seq(deleteClause, DeleteAggregated())
      case returnClause: Return if isReturnDependsOnDelete => Seq(returnClause, DeleteAggregated())
      case n                                               => Seq(n)
    } ++ maybeEmptyReturn
  }

  case class EmptyReturn() extends ASTNode {
    override def position: InputPosition = InputPosition.NONE
  }

  case class DeleteAggregated() extends ASTNode {
    override def position: InputPosition = InputPosition.NONE
  }

  private def walkClause(node: ASTNode): Unit = {
    node match {
      case matchClause: Match =>
        MatchWalker.walkClause(context, g, matchClause)
      case unwindClause: Unwind =>
        UnwindWalker.walkClause(context, g, unwindClause)
      case createClause: Create =>
        CreateWalker.walkClause(context, g, createClause)
      case mergeClause: Merge =>
        MergeWalker.walkClause(context, g, mergeClause)
      case deleteClause: Delete =>
        DeleteWalker.walkClause(context, g, deleteClause)
      case _: DeleteAggregated =>
        DeleteWalker.deleteAggregated(context, g)
      case SetClause(_) | Remove(_) =>
        SetWalker.walkClause(context, g, node)
      case projectionClause: ProjectionClause =>
        ProjectionWalker.walk(context, g, projectionClause)
      case callClause: UnresolvedCall =>
        CallWalker.walk(context, g, callClause)
      case _: EmptyReturn =>
        g.barrier().limit(0)
      case _ =>
        context.unsupported("clause", node)
    }
  }

  def returnDependsOnDelete(clauses: Seq[Clause]): Boolean = {
    val returnClauses = clauses.filter(_.isInstanceOf[Return])
    val deleteClauses = clauses.filter(_.isInstanceOf[Delete])

    if (deleteClauses.size > 1) {
      context.unsupported("query. Multiple delete clauses", deleteClauses)
    }

    val deleteDependencies = deleteClauses.flatMap {
      case Delete(expressions, _) => expressions.flatMap(_.dependencies)
      case _                      => Seq()
    }

    val returnDependencies = returnClauses.flatMap {
      case Return(_, returnItems, _, _, _, _) => returnItems.items
      case _                                  => Seq()
    }.flatMap {
      case AliasedReturnItem(FunctionInvocation(_, FunctionName("labels"), _, args), _)
          if deleteDependencies.intersect(args).nonEmpty =>
        throw new EntityNotFound("Deleted entity label access " + args)
      case AliasedReturnItem(Property(expression, _), _) if deleteDependencies.contains(expression) =>
        throw new EntityNotFound("Deleted entity property access " + expression)
      case n => n.expression.dependencies
    }

    returnDependencies.intersect(deleteDependencies).nonEmpty
  }
}
