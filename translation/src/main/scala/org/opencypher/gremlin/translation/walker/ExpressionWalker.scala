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

import org.apache.tinkerpop.gremlin.process.traversal.Scope
import org.apache.tinkerpop.gremlin.structure.{Column, Vertex}
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.symbols._

import scala.collection.JavaConverters._
import scala.collection.immutable.NumericRange

/**
  * AST walker that handles translation
  * of evaluable expression nodes in the Cypher AST.
  */
object ExpressionWalker {
  def walk[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Expression): Unit = {
    new ExpressionWalker(context, g).walk(node)
  }

  def walkLocal[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Expression): GremlinSteps[T, P] = {
    new ExpressionWalker(context, g).walkLocal(node)
  }

  def walkProperty[T, P](
      context: WalkerContext[T, P],
      g: GremlinSteps[T, P],
      key: String,
      value: Expression): GremlinSteps[T, P] = {
    new ExpressionWalker(context, g).walkProperty(key, value)
  }
}

private class ExpressionWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {
  def walk(node: Expression): Unit = {
    g.map(walkLocal(node))
  }

  private def __ = g.start()

  private def walkLocal(expression: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()

    expression match {
      case Variable(varName) =>
        __.select(varName)

      case Property(expr, PropertyKeyName(keyName: String)) =>
        val typ = context.expressionTypes.get(expr)
        val maybeExtractStep: Option[String => GremlinSteps[T, P]] = typ match {
          case Some(NodeType.instance)         => Some(__.values(_))
          case Some(RelationshipType.instance) => Some(__.values(_))
          case Some(MapType.instance)          => Some(__.select(_))
          case _                               => None
        }
        maybeExtractStep.map { extractStep =>
          walkLocal(expr)
            .map(
              notNull(
                __.coalesce(
                  extractStep(keyName),
                  __.constant(NULL)
                ),
                context))
        }.getOrElse {
          val key = StringLiteral(keyName)(InputPosition.NONE)
          asList(expr, key).map(CustomFunction.containerIndex())
        }

      case HasLabels(expr, List(LabelName(label))) =>
        walkLocal(expr)
          .map(notNull(anyMatch(__.hasLabel(label)), context))

      case Equals(lhs, rhs)             => comparison(lhs, rhs, p.isEq)
      case Not(Equals(lhs, rhs))        => comparison(lhs, rhs, p.neq)
      case LessThan(lhs, rhs)           => comparison(lhs, rhs, p.lt)
      case LessThanOrEqual(lhs, rhs)    => comparison(lhs, rhs, p.lte)
      case GreaterThan(lhs, rhs)        => comparison(lhs, rhs, p.gt)
      case GreaterThanOrEqual(lhs, rhs) => comparison(lhs, rhs, p.gte)
      case StartsWith(lhs, rhs)         => comparison(lhs, rhs, p.startsWith)
      case EndsWith(lhs, rhs)           => comparison(lhs, rhs, p.endsWith)
      case Contains(lhs, rhs)           => comparison(lhs, rhs, p.contains)

      case In(lhs, rhs) =>
        membership(lhs, rhs)

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
        val rhsName = context.generateName()
        __.choose(
          __.or(copy(lhsT).is(p.isEq(NULL)), copy(rhsT).is(p.isEq(NULL))),
          __.constant(NULL),
          __.choose(
            copy(rhsT).as(rhsName).map(lhsT).where(p.neq(rhsName)),
            __.constant(true),
            __.constant(false)
          )
        )

      case Add(lhs, rhs) =>
        (typeOf(lhs), typeOf(rhs)) match {
          case (_: IntegerType, _: IntegerType) =>
            math(lhs, rhs, "+")
          case _ =>
            asList(lhs, rhs).map(CustomFunction.plus())
        }

      case Subtract(lhs, rhs) => math(lhs, rhs, "-")
      case Multiply(lhs, rhs) => math(lhs, rhs, "*")
      case Divide(lhs, rhs)   => math(lhs, rhs, "/")
      case Pow(lhs, rhs)      => math(lhs, rhs, "^")
      case Modulo(lhs, rhs)   => math(lhs, rhs, "%")

      case ContainerIndex(expr, idx) =>
        (typeOf(expr), idx) match {
          case (_: ListType, l: IntegerLiteral) if l.value >= 0 =>
            walkLocal(expr).coalesce(
              __.range(Scope.local, l.value, l.value + 1),
              __.constant(NULL)
            )
          case _ =>
            asList(expr, idx).map(CustomFunction.containerIndex())
        }

      case ListSlice(expr, maybeFrom, maybeTo) =>
        val fromIdx = maybeFrom.getOrElse(SignedDecimalIntegerLiteral("0")(InputPosition.NONE))
        val toIdx = maybeTo.getOrElse(SignedDecimalIntegerLiteral("-1")(InputPosition.NONE))
        (fromIdx, toIdx) match {
          case (from: IntegerLiteral, to: IntegerLiteral)
              if from.value == to.value || (from.value > to.value && to.value >= 0) =>
            walkLocal(expr).limit(0).fold()
          case (from: IntegerLiteral, to: IntegerLiteral) if from.value >= 0 && (to.value >= 1 || to.value == -1) =>
            val rangeT = __.range(Scope.local, from.value, to.value)
            if (to.value - from.value == 1) {
              rangeT.fold()
            }
            walkLocal(expr).coalesce(rangeT, __.constant(NULL))
          case _ =>
            asList(expr, fromIdx, toIdx).map(CustomFunction.listSlice())
        }

      case FunctionInvocation(_, FunctionName(fnName), distinct, args) =>
        val traversals = args.map(walkLocal)
        val traversal = fnName.toLowerCase match {
          case "abs"           => traversals.head.math("abs(_)")
          case "coalesce"      => __.coalesce(traversals.init.map(_.is(p.neq(NULL))) :+ traversals.last: _*)
          case "exists"        => traversals.head.map(anyMatch(__.is(p.neq(NULL))))
          case "id"            => traversals.head.map(notNull(__.id(), context))
          case "keys"          => traversals.head.properties().key().fold()
          case "labels"        => traversals.head.label().is(p.neq(Vertex.DEFAULT_LABEL)).fold()
          case "length"        => traversals.head.map(CustomFunction.length())
          case "nodes"         => traversals.head.map(CustomFunction.nodes())
          case "properties"    => traversals.head.map(notNull(__.map(CustomFunction.properties()), context))
          case "range"         => range(args)
          case "relationships" => traversals.head.map(CustomFunction.relationships())
          case "size"          => traversals.head.map(CustomFunction.size())
          case "sqrt"          => traversals.head.math("sqrt(_)")
          case "type"          => traversals.head.map(notNull(__.label().is(p.neq(Vertex.DEFAULT_LABEL)), context))
          case "toboolean"     => traversals.head.map(CustomFunction.convertToBoolean())
          case "tofloat"       => traversals.head.map(CustomFunction.convertToFloat())
          case "tointeger"     => traversals.head.map(CustomFunction.convertToInteger())
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

      case ListLiteral(expressions @ _ :: _) =>
        asList(expressions: _*)

      case MapExpression(items @ _ :: _) =>
        val keys = items.map(_._1.name)
        val traversal = __.project(keys: _*)
        items.map(_._2).map(walkLocal).foreach(traversal.by)
        traversal

      case _: Parameter =>
        __.coalesce(
          __.constant(expressionValue(expression, context)),
          __.constant(NULL)
        )

      case _ =>
        __.constant(expressionValue(expression, context))
    }
  }

  def walkProperty(key: String, value: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    val traversal = walkLocal(value)
    g.choose(
      g.start().map(traversal).is(p.neq(NULL)).unfold(),
      g.start().property(key, traversal),
      g.start().sideEffect(g.start().properties(key).drop())
    )
  }

  private def typeOf(expr: Expression): CypherType = {
    context.expressionTypes.getOrElse(expr, AnyType.instance)
  }

  private def copy(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    __.map(traversal)
  }

  private def anyMatch(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    __.choose(
      traversal,
      __.constant(true),
      __.constant(false)
    )
  }

  private def asList(expressions: Expression*): GremlinSteps[T, P] = {
    val keys = expressions.map(_ => context.generateName())
    val traversal = __.project(keys: _*)
    expressions.map(walkLocal).foreach(traversal.by)
    traversal.select(Column.values)
  }

  private def bothNotNull(
      lhs: Expression,
      rhs: Expression,
      ifTrue: GremlinSteps[T, P],
      rhsName: String): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()

    val lhsT = walkLocal(lhs)
    val rhsT = walkLocal(rhs)

    rhsT
      .as(rhsName)
      .map(lhsT)
      .choose(
        __.or(__.is(p.isEq(NULL)), __.select(rhsName).is(p.isEq(NULL))),
        __.constant(NULL),
        ifTrue
      )
  }

  private def comparison(lhs: Expression, rhs: Expression, predicate: String => P): GremlinSteps[T, P] = {
    val rhsName = context.generateName()
    val traversal = anyMatch(__.where(predicate(rhsName)))
    bothNotNull(lhs, rhs, traversal, rhsName)
  }

  private def membership(lhs: Expression, rhs: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    val lhsT = walkLocal(lhs)
    val rhsT = walkLocal(rhs)
    val rhsName = context.generateName()

    rhsT
      .as(rhsName)
      .map(lhsT)
      .choose(
        __.select(rhsName).is(p.isEq(NULL)),
        __.constant(NULL),
        __.choose(
          __.or(
            __.and(
              __.is(p.isEq(NULL)),
              __.select(rhsName).unfold().limit(1)
            ),
            __.and(
              __.constant(NULL).where(p.within(rhsName)),
              __.not(__.where(p.within(rhsName)))
            )
          ),
          __.constant(NULL),
          anyMatch(__.where(p.within(rhsName)))
        )
      )
  }

  private def math(lhs: Expression, rhs: Expression, op: String): GremlinSteps[T, P] = {
    val rhsName = context.generateName().replace(" ", "_") // name limited by MathStep#VARIABLE_PATTERN
    val traversal = __.math(s"_ $op $rhsName")
    bothNotNull(lhs, rhs, traversal, rhsName)
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
    PatternWalker.walk(contextWhere, select, relationshipChain)
    maybePredicate.foreach(WhereWalker.walk(contextWhere, select, _))

    if (projection.isInstanceOf[PathExpression]) {
      select.path()
    }

    val name = contextWhere.generateName()
    g.sideEffect(select.aggregate(name)).barrier()

    name
  }
}
