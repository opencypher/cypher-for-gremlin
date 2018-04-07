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

import org.apache.tinkerpop.gremlin.structure.Vertex
import org.neo4j.cypher.internal.frontend.v3_3.ast._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils.expressionValue
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}

import scala.collection.immutable.NumericRange

/**
  * AST walker that handles translation
  * of the `UNWIND` clause nodes in the Cypher AST.
  */
object UnwindWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Unwind): Unit = {
    new UnwindWalker(context, g).walkClause(node)
  }
}

private class UnwindWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  private val injectHardLimit = 10000

  def walkClause(node: Unwind): Unit = {
    val p = context.dsl.predicates()

    if (context.isFirstStatement) {
      context.markFirstStatement()
    } else {
      val p = context.dsl.predicates()
      g.is(p.neq(Tokens.START))
    }

    val Unwind(expression, Variable(varName)) = node
    expression match {
      case ListLiteral(list) =>
        val values = list
          .map(expressionValue(_, context))
          .asInstanceOf[Seq[Object]]
        g.inject(values: _*).as(varName)
      case FunctionInvocation(_, FunctionName(fnName), _, args) if "range" == fnName.toLowerCase =>
        val range: NumericRange[Long] = args match {
          case Seq(start: IntegerLiteral, end: IntegerLiteral) =>
            NumericRange.inclusive(start.value, end.value, 1)
          case Seq(start: IntegerLiteral, end: IntegerLiteral, step: IntegerLiteral) =>
            NumericRange.inclusive(start.value, end.value, step.value)
        }
        walkRange(range, varName)
      case FunctionInvocation(_, FunctionName(fnName), _, Vector(Variable(funArg))) if "labels" == fnName.toLowerCase =>
        g.select(funArg).label().is(p.neq(Vertex.DEFAULT_LABEL)).as(varName)
      case Variable(name) =>
        g.select(name).unfold().as(varName)
      case Null() =>
        g.inject(Collections.emptyList).unfold().as(varName)
      case _: Expression =>
        g.inject(expressionValue(expression, context)).unfold().as(varName)
    }
  }

  private def walkRange(range: NumericRange[Long], varName: String) = {
    context.precondition(
      range.length <= injectHardLimit,
      s"Range is too big (must be less than or equal to $injectHardLimit)",
      range
    )

    if (range.step == 1) {
      val rangeLabel = context.generateName()
      g.inject(Tokens.START)
        .repeat(g.start().loops().aggregate(rangeLabel))
        .times((range.end + 1).toInt)
        .cap(rangeLabel)
        .unfold()
        .skip(range.start)
        .limit(range.end - range.start + 1)
        .as(varName)
    } else {
      val numbers = range.asInstanceOf[Seq[Object]]
      g.inject(numbers: _*).as(varName)
    }
  }
}
