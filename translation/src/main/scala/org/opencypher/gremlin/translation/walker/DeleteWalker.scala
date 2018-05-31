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

import java.util.function.Function

import org.apache.tinkerpop.gremlin.process.traversal.{P, Traverser}
import org.apache.tinkerpop.gremlin.structure.{Column, Element}
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.{GremlinPredicates, GremlinSteps, Tokens}
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.Expression
import org.opencypher.v9_0.util.symbols.{AnyType, NodeType, RelationshipType}

object DeleteWalker {
  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Delete): Unit = {
    new DeleteWalker(context, g).walkClause(node)
  }
}

class DeleteWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  private type SemanticCheck = (GremlinSteps[T, P], Expression) => GremlinSteps[T, P]

  private def __ = g.start()

  def walkClause(node: Delete): Unit = {
    val Delete(expressions, detach) = node
    var check: Option[SemanticCheck] = None

    if (!detach) {
      val deletedItemsAlias = context.generateName();
      g.barrier()
        .sideEffect(project(expressions).aggregate(deletedItemsAlias))
      check = Some(ensureNodeHasNoEdges(deletedItemsAlias))
    }

    g.barrier()
      .sideEffect(project(expressions, check).drop())
  }

  private def project(expressions: Seq[Expression], check: Option[SemanticCheck] = None) = {
    val p = context.dsl.predicates()
    val sideEffect = g.start().project(expressions.map(_ => context.generateName()): _*)
    expressions.foreach(expr => {
      var value = ExpressionWalker.walkLocal(context, g, expr)
      check.foreach { f =>
        value = f.apply(value, expr)
      }
      sideEffect.by(value)
    })
    sideEffect
      .select(Column.values)
      .unfold()
      .unfold() // Unwraps paths
      .is(p.neq(NULL))
  }

  def ensureNodeHasNoEdges(deletedItemsAlias: String): SemanticCheck =
    (traversal: GremlinSteps[T, P], expr: Expression) => {
      val p = context.dsl.predicates()
      val typ = context.expressionTypes.getOrElse(expr, AnyType.instance)
      typ match {
        case NodeType.instance =>
          traversal.choose(
            __.or(
              __.is(p.isEq(NULL)),
              __.bothE().where(p.without(deletedItemsAlias)).count().is(p.isEq(0))
            ),
            __.identity(),
            __.map(CustomFunction.raise(errorMapper))
          )
        case _ => traversal
      }
    }

  private val errorMapper = new Function[Traverser[_], RuntimeException] {
    override def apply(t: Traverser[_]): RuntimeException = {
      val id = t.get().asInstanceOf[Element].id()
      new IllegalStateException(
        ("Cannot delete node<%s>, because it still has relationships." +
          " To delete this node, you must first delete its relationships.").format(id))
    }
  }
}
