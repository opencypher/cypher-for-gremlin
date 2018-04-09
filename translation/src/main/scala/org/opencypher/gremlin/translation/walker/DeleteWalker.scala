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

import org.apache.tinkerpop.gremlin.structure.Column
import org.neo4j.cypher.internal.frontend.v3_3.ast.{Delete, Variable}
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}

object DeleteWalker {
  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Delete): Unit = {
    new DeleteWalker(context, g).walkClause(node)
  }
}

class DeleteWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Delete): Unit = {
    val Delete(expressions, _) = node
    val aliases = expressions.map {
      case Variable(name) =>
        name
      case n =>
        context.unsupported("delete expression", n)
    }

    val p = context.dsl.predicates()

    val traversal = g.start().select(aliases: _*)
    if (aliases.length > 1) {
      traversal.select(Column.values)
    }

    g.barrier()
      .sideEffect(
        traversal
          .unfold()
          .is(p.neq(Tokens.NULL))
          .drop()
      )
  }
}
