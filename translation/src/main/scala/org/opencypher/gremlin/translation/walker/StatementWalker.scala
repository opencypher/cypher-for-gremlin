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

import org.neo4j.cypher.internal.frontend.v3_2.ast._
import org.opencypher.gremlin.translation.Tokens.START
import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.walker.NodeUtils.expressionValue

import scala.collection.mutable

/**
  * AST walker that starts translation of the Cypher AST.
  */
object StatementWalker {
  def walk[T, P](context: StatementContext[T, P], node: Statement) {
    val g = context.dsl.translationBuilder()
    new StatementWalker(context, g).walk(node)
  }
}

class StatementWalker[T, P](context: StatementContext[T, P], g: TranslationBuilder[T, P]) {

  def walk(node: Statement) {
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

  def walkUnion(node: Union) {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    val subGs = mutable.ArrayBuffer.empty[TranslationBuilder[T, P]]
    for (query <- flattenUnion(Vector(), node)) {
      val subG = g.start()
      new StatementWalker(context, subG).walkSingle(query)
      subGs += subG
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

  def walkSingle(node: SingleQuery) {
    val clauses = node.clauses
    walkFirstClause(clauses.head)
    clauses.tail.foreach(walkClause)

    val returnClauses = clauses
      .find(_.isInstanceOf[Return])
      .map(_.asInstanceOf[Return])

    if (returnClauses.nonEmpty) {
      returnClauses.foreach(ReturnWalker.walk(context, g, _))
    } else {
      g.barrier().limit(0)
    }
  }

  private def walkFirstClause(node: Clause) {
    node match {
      case withClause: With =>
        if (context.isFirstStatement) {
          context.markFirstStatement()
          g.inject(START)
        }
        withClause.returnItems.items.foldLeft(g) { (g, item) =>
          val AliasedReturnItem(expr, Variable(alias)) = item
          g.constant(expressionValue(expr, context)).limit(1).as(alias)
        }
      case _: Return => // Handled elsewhere
      case matchClause: Match =>
        MatchWalker.walkClause(context, g, matchClause)
      case unwindClause: Unwind =>
        UnwindWalker.walkClause(context, g, unwindClause)
      case createClause: Create =>
        CreateWalker.walkClause(context, g, createClause)
      case mergeClause: Merge =>
        MergeWalker.walkClause(context, g, mergeClause)
      case _ =>
        context.unsupported("first clause", node)
    }
  }

  private def walkClause(node: Clause) {
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
      case SetClause(_) | Remove(_) =>
        SetWalker.walkClause(context, g, node)
      case withClause: With =>
        WithWalker.walkClause(context, g, withClause)
      case _: Return => // Handled elsewhere
      case _ =>
        context.unsupported("clause", node)
    }
  }
}
