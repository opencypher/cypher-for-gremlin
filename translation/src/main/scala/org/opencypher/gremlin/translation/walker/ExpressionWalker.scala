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

import org.apache.tinkerpop.gremlin.structure.{Column, Vertex}
import org.neo4j.cypher.internal.frontend.v3_3.ast._
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils.expressionValue
import org.opencypher.gremlin.traversal.CustomFunction

import scala.collection.JavaConverters._
import scala.collection.immutable.NumericRange

/**
  * AST walker that handles translation
  * of evaluable expression nodes in the Cypher AST.
  */
object ExpressionWalker {
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Expression): Unit = {
    new ExpressionWalker(context, g).walk(node)
  }

  def walkLocal[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Expression): GremlinSteps[T, P] = {
    new ExpressionWalker(context, g).walkLocal(node)
  }
}

private class ExpressionWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {
  def walk(node: Expression): Unit = {
    g.map(walkLocal(node))
  }

  private def __ = g.start()

  private def walkLocal(expression: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()

    expression match {
      case Variable(varName) =>
        __.select(varName)

      case Property(Variable(varName), PropertyKeyName(keyName: String)) =>
        __.select(varName)
          .map(
            notNull(
              __.coalesce(
                __.values(keyName),
                __.constant(NULL)
              )))

      case HasLabels(Variable(varName), List(LabelName(label))) =>
        __.select(varName)
          .map(notNull(anyMatch(__.hasLabel(label))))

      case IsNull(expr) =>
        walkLocal(expr).map(anyMatch(__.is(p.isEq(NULL))))

      case IsNotNull(expr) =>
        walkLocal(expr).map(anyMatch(__.is(p.neq(NULL))))

      case Not(rhs) =>
        val rhsT = walkLocal(rhs)
        __.choose(
          copy(rhsT).is(p.isEq(NULL)),
          __.constant(NULL),
          __.choose(
            copy(rhsT).is(p.isEq(true)),
            __.constant(false),
            __.constant(true)
          )
        )

      case Ands(ands) =>
        val traversals = ands.map(walkLocal).toSeq
        __.choose(
          __.and(traversals.map(copy).map(_.is(p.isEq(true))): _*),
          __.constant(true),
          __.choose(
            __.or(traversals.map(copy).map(_.is(p.isEq(false))): _*),
            __.constant(false),
            __.constant(NULL)
          )
        )

      case Ors(ors) =>
        val traversals = ors.map(walkLocal).toSeq
        __.choose(
          __.or(traversals.map(copy).map(_.is(p.isEq(true))): _*),
          __.constant(true),
          __.choose(
            __.and(traversals.map(copy).map(_.is(p.isEq(false))): _*),
            __.constant(false),
            __.constant(NULL)
          )
        )

      case Xor(lhs, rhs) =>
        val lhsT = walkLocal(lhs)
        val rhsT = walkLocal(rhs)
        __.choose(
          __.or(copy(lhsT).is(p.isEq(NULL)), copy(rhsT).is(p.isEq(NULL))),
          __.constant(NULL),
          __.choose(
            copy(rhsT).as(TEMP).map(lhsT).where(p.neq(TEMP)),
            __.constant(true),
            __.constant(false)
          )
        )

      case Add(lhs, rhs)      => math(lhs, rhs, "+")
      case Subtract(lhs, rhs) => math(lhs, rhs, "-")
      case Multiply(lhs, rhs) => math(lhs, rhs, "*")
      case Divide(lhs, rhs)   => math(lhs, rhs, "/")
      case Pow(lhs, rhs)      => math(lhs, rhs, "^")
      case Modulo(lhs, rhs)   => math(lhs, rhs, "%")

      case ContainerIndex(expr, idx) =>
        val index = expressionValue(idx, context)
        walkLocal(expr).map(CustomFunction.containerIndex(index))

      case FunctionInvocation(_, FunctionName(fnName), distinct, args) =>
        val traversals = args.map(walkLocal)
        val traversal = fnName.toLowerCase match {
          case "abs"           => traversals.head.math("abs(_)")
          case "exists"        => traversals.head.map(anyMatch(__.is(p.neq(NULL))))
          case "coalesce"      => __.coalesce(traversals.init.map(_.is(p.neq(NULL))) :+ traversals.last: _*)
          case "id"            => traversals.head.map(notNull(__.id()))
          case "keys"          => traversals.head.valueMap().select(Column.keys)
          case "labels"        => traversals.head.label().is(p.neq(Vertex.DEFAULT_LABEL)).fold()
          case "length"        => traversals.head.map(CustomFunction.length())
          case "nodes"         => traversals.head.map(CustomFunction.nodes())
          case "properties"    => traversals.head.map(notNull(__.map(CustomFunction.properties())))
          case "range"         => range(args)
          case "relationships" => traversals.head.map(CustomFunction.relationships())
          case "size"          => traversals.head.map(CustomFunction.size())
          case "sqrt"          => traversals.head.math("sqrt(_)")
          case "type"          => traversals.head.map(notNull(__.label().is(p.neq(Vertex.DEFAULT_LABEL))))
          case "toboolean"     => traversals.head.map(CustomFunction.convertToBoolean())
          case "tofloat"       => traversals.head.map(CustomFunction.convertToFloat())
          case "tointeger"     => traversals.head.map(CustomFunction.convertToIntegerType())
          case "tostring"      => traversals.head.map(CustomFunction.convertToString())
          case _ =>
            throw new SyntaxException(s"Unknown function '$fnName'")
        }
        if (distinct) {
          throw new SyntaxException("Invalid use of DISTINCT with function '" + fnName + "'")
        }
        traversal

      case ListComprehension(ExtractScope(_, _, Some(function)), target) if function.dependencies.size == 1 =>
        val targetT = walkLocal(target)
        val functionT = walkLocal(function)

        val Variable(dependencyName) = function.dependencies.head
        targetT.unfold().as(dependencyName).map(functionT).fold()

      case PatternComprehension(_, RelationshipsPattern(relationshipChain), maybeExpression, projection, _) =>
        val varName = patternComprehensionPath(relationshipChain, maybeExpression, projection)
        val traversal = __.select(varName)

        projection match {
          case PathExpression(_) =>
            traversal.map(CustomFunction.pathComprehension())
          case expression: Expression =>
            val functionT = walkLocal(expression)
            if (expression.dependencies.isEmpty) {
              traversal.unfold().map(functionT).fold()
            } else if (expression.dependencies.size == 1) {
              val Variable(dependencyName) = expression.dependencies.head
              traversal.unfold().as(dependencyName).map(functionT).fold()
            } else {
              context.unsupported("pattern comprehension with multiple arguments", expression)
            }
        }

      case _ =>
        __.constant(expressionValue(expression, context))
    }
  }

  private def copy(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    __.map(traversal)
  }

  private def notNull(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    __.choose(p.neq(NULL), traversal, __.constant(NULL))
  }

  private def anyMatch(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    __.choose(
      traversal,
      __.constant(true),
      __.constant(false)
    )
  }

  private def math(lhs: Expression, rhs: Expression, op: String): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()

    val lhsT = walkLocal(lhs)
    val rhsT = walkLocal(rhs)

    val lhsName = context.generateName().replace(" ", "_") // name limited by MathStep#VARIABLE_PATTERN

    lhsT
      .as(lhsName)
      .map(rhsT)
      .choose(
        __.or(__.is(p.isEq(NULL)), __.select(lhsName).is(p.isEq(NULL))),
        __.constant(NULL),
        __.math(s"$lhsName $op _")
      )
  }

  private val injectHardLimit = 10000

  private def range(rangeArgs: Seq[Expression]): GremlinSteps[T, P] = {
    val range: NumericRange[Long] = rangeArgs match {
      case Seq(start: IntegerLiteral, end: IntegerLiteral) =>
        NumericRange.inclusive(start.value, end.value, 1)
      case Seq(start: IntegerLiteral, end: IntegerLiteral, step: IntegerLiteral) =>
        NumericRange.inclusive(start.value, end.value, step.value)
    }

    context.precondition(
      range.length <= injectHardLimit,
      s"Range is too big (must be less than or equal to $injectHardLimit)",
      range
    )

    if (range.step == 1) {
      val rangeLabel = context.generateName()
      __.repeat(__.start().loops().aggregate(rangeLabel))
        .times((range.end + 1).toInt)
        .cap(rangeLabel)
        .unfold()
        .skip(range.start)
        .limit(range.end - range.start + 1)
        .fold()
    } else {
      val numbers = range.asInstanceOf[Seq[Object]]
      __.constant(numbers.asJava)
    }
  }

  private def patternComprehensionPath(
      relationshipChain: RelationshipChain,
      maybePredicate: Option[Expression],
      projection: Expression): String = {
    val select = __
    val contextWhere = context.copy()
    WhereWalker.walkRelationshipChain(contextWhere, select, relationshipChain)
    maybePredicate.foreach(WhereWalker.walk(contextWhere, select, _))

    if (projection.isInstanceOf[PathExpression]) {
      select.path()
    }

    val name = contextWhere.generateName()
    g.sideEffect(
      select.aggregate(name)
    )

    name
  }
}
