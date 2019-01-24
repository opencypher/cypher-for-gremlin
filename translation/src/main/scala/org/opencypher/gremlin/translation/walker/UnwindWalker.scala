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

import java.util.Collections

import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._

/**
  * AST walker that handles translation
  * of the `UNWIND` clause nodes in the Cypher AST.
  */
object UnwindWalker {

  def walkClause[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Unwind): Unit = {
    new UnwindWalker(context, g).walkClause(node)
  }
}

private class UnwindWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Unwind): Unit = {
    val Unwind(expression, Variable(varName)) = node
    expression match {
      case Null() =>
        context.markFirstStatement()
        g.inject(Collections.emptyList).unfold().as(varName)
      case _: Expression =>
        ensureFirstStatement(g, context)
        ExpressionWalker.walk(context, g, expression)
        g.unfold().as(varName)
    }
  }
}
