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
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalOptionParent.Pick
import org.apache.tinkerpop.gremlin.structure.{Column, Vertex}
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.CypherExceptions.INVALID_RANGE
import org.opencypher.gremlin.translation.exception.{ArgumentException, SyntaxException}
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.symbols._

/**
  * AST walker that handles translation
  * of evaluable expression nodes in the Cypher AST.
  */
object ExpressionWalker {
  def walk[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Expression): Unit = {
    new ExpressionWalker(context, g).walk(node)
  }

  def walkLocal[T, P](
      context: WalkerContext[T, P],
      g: GremlinSteps[T, P],
      node: Expression,
      maybeAlias: Option[String] = None): GremlinSteps[T, P] = {
    new ExpressionWalker(context, g).walkLocal(node, maybeAlias)
  }

  def walkProperty[T, P](
      context: WalkerContext[T, P],
      g: GremlinSteps[T, P],
      cypherType: CypherType,
      key: String,
      value: Expression): GremlinSteps[T, P] = {
    new ExpressionWalker(context, g).walkProperty(cypherType, key, value)
  }
}

private class ExpressionWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {
  private val p = context.dsl.predicates()

  def walk(node: Expression): Unit = {
    g.flatMap(walkLocal(node))
  }

  private def __ = g.start()

  private def walkLocal(expression: Expression): GremlinSteps[T, P] = {
    walkLocal(expression, None)
  }

  private def walkLocal(expression: Expression, maybeAlias: Option[String]): GremlinSteps[T, P] = {
    expression match {
      case Variable(varName) =>
        __.select(varName)

      case Property(expr, PropertyKeyName(keyName: String)) =>
        val typ = context.expressionTypes.get(expr)
        val maybeExtractStep: Option[String => GremlinSteps[T, P]] = typ match {
          case Some(_: NodeType)         => Some(__.values(_))
          case Some(_: RelationshipType) => Some(__.values(_))
          case Some(_: MapType)          => Some(__.select(_))
          case _                         => None
        }
        maybeExtractStep.map { extractStep =>
          walkLocal(expr, maybeAlias)
            .flatMap(notNull(emptyToNull(extractStep(keyName), context), context))
        }.getOrElse {
          val key = StringLiteral(keyName)(InputPosition.NONE)
          asList(expr, key).map(CustomFunction.cypherContainerIndex())
        }

      case HasLabels(expr, List(LabelName(label))) =>
        walkLocal(expr, maybeAlias)
          .flatMap(notNull(anyMatch(__.hasLabel(label)), context))

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
        walkLocal(expr, maybeAlias).flatMap(anyMatch(__.is(p.isEq(NULL))))

      case IsNotNull(expr) =>
        walkLocal(expr, maybeAlias).flatMap(anyMatch(__.is(p.neq(NULL))))

      case Not(rhs) =>
        val rhsT = walkLocal(rhs, maybeAlias)
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
        val traversals = ands.map(walkLocal(_, maybeAlias)).toSeq
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
        val traversals = ors.map(walkLocal(_, maybeAlias)).toSeq
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
        val lhsT = walkLocal(lhs, maybeAlias)
        val rhsT = walkLocal(rhs, maybeAlias)
        val rhsName = context.generateName()
        __.choose(
          __.or(copy(lhsT).is(p.isEq(NULL)), copy(rhsT).is(p.isEq(NULL))),
          __.constant(NULL),
          __.choose(
            copy(rhsT).as(rhsName).flatMap(lhsT).where(p.neq(rhsName)),
            __.constant(true),
            __.constant(false)
          )
        )

      case Add(lhs, rhs) =>
        (typeOf(lhs), typeOf(rhs)) match {
          case (_: ListType, _) | (_, _: ListType) =>
            listConcat(lhs, rhs)
          case (_: IntegerType, _: IntegerType) =>
            math(lhs, rhs, "+")
          case _ =>
            asList(lhs, rhs).map(CustomFunction.cypherPlus())
        }

      case Subtract(lhs, rhs) => math(lhs, rhs, "-")
      case Multiply(lhs, rhs) => math(lhs, rhs, "*")
      case Divide(lhs, rhs)   => math(lhs, rhs, "/")
      case Pow(lhs, rhs)      => math(lhs, rhs, "^")
      case Modulo(lhs, rhs)   => math(lhs, rhs, "%")

      case ContainerIndex(expr, idx) =>
        (typeOf(expr), idx) match {
          case (_: ListType, l: IntegerLiteral) if l.value >= 0 =>
            walkLocal(expr, maybeAlias).flatMap(
              emptyToNull(
                __.range(Scope.local, l.value, l.value + 1),
                context
              ))
          case _ =>
            asList(expr, idx).map(CustomFunction.cypherContainerIndex())
        }

      case ListSlice(expr, maybeFrom, maybeTo) =>
        val fromIdx = maybeFrom.getOrElse(SignedDecimalIntegerLiteral("0")(InputPosition.NONE))
        val toIdx = maybeTo.getOrElse(SignedDecimalIntegerLiteral("-1")(InputPosition.NONE))
        (fromIdx, toIdx) match {
          case (from: IntegerLiteral, to: IntegerLiteral)
              if from.value == to.value || (from.value > to.value && to.value >= 0) =>
            walkLocal(expr, maybeAlias).limit(0).fold()
          case (from: IntegerLiteral, to: IntegerLiteral) if from.value >= 0 && (to.value >= 1 || to.value == -1) =>
            val rangeT = __.range(Scope.local, from.value, to.value)
            if (to.value - from.value == 1) {
              rangeT.fold()
            }
            walkLocal(expr, maybeAlias)
              .flatMap(emptyToNull(rangeT, context))
          case _ =>
            asList(expr, fromIdx, toIdx).map(CustomFunction.cypherListSlice())
        }

      case FunctionInvocation(_, FunctionName(fnName), distinct, args) =>
        def onEntity = typeOf(args.head).isInstanceOf[NodeType] || typeOf(args.head).isInstanceOf[RelationshipType]

        lazy val a3 = args.size == 3
        val traversals = args.map(walkLocal(_, maybeAlias))
        val traversal = fnName.toLowerCase match {
          case "abs"              => traversals.head.math("abs(_)")
          case "coalesce"         => __.coalesce(traversals.init.map(_.is(p.neq(NULL))) :+ traversals.last: _*)
          case "endnode"          => traversals.head.flatMap(notNull(__.inV(), context))
          case "exists"           => traversals.head.flatMap(anyMatch(__.is(p.neq(NULL))))
          case "head"             => traversals.head.flatMap(emptyToNull(__.limit(Scope.local, 1), context))
          case "id"               => traversals.head.flatMap(notNull(__.id(), context))
          case "keys" if onEntity => traversals.head.map(__.properties().key().fold())
          case "keys"             => traversals.head.select(Column.keys)
          case "labels"           => traversals.head.map(__.label().is(p.neq(Vertex.DEFAULT_LABEL)).fold())
          case "length"           => traversals.head.count(Scope.local).math("(_-1)/2")
          case "last"             => traversals.head.flatMap(emptyToNull(__.tail(Scope.local, 1), context))
          case "nodes"            => traversals.head.flatMap(filterElements(args, includeNodes = true))
          case "properties"       => traversals.head.flatMap(properties(args))
          case "range"            => range(args)
          case "relationships"    => traversals.head.flatMap(filterElements(args, includeNodes = false))
          case "size"             => traversals.head.flatMap(size(args))
          case "startnode"        => traversals.head.flatMap(notNull(__.outV(), context))
          case "sqrt"             => traversals.head.math("sqrt(_)")
          case "tail"             => traversals.head.flatMap(__.range(Scope.local, 1, -1))
          case "type"             => traversals.head.flatMap(notNull(__.label().is(p.neq(Vertex.DEFAULT_LABEL)), context))
          case "reverse"          => traversals.head.map(CustomFunction.cypherReverse())
          case "split"            => asList(args(0), args(1)).map(CustomFunction.cypherSplit())
          case "substring" if a3  => asList(args(0), args(1), args(2)).map(CustomFunction.cypherSubstring())
          case "substring"        => asList(args(0), args(1)).map(CustomFunction.cypherSubstring())
          case "toupper"          => traversals.head.map(CustomFunction.cypherToUpper())
          case "tolower"          => traversals.head.map(CustomFunction.cypherToLower())
          case "toboolean"        => traversals.head.map(CustomFunction.cypherToBoolean())
          case "tofloat"          => traversals.head.map(CustomFunction.cypherToFloat())
          case "tointeger"        => traversals.head.map(CustomFunction.cypherToInteger())
          case "tostring"         => traversals.head.map(CustomFunction.cypherToString())
          case _ =>
            throw new SyntaxException(s"Unknown function '$fnName'")
        }
        if (distinct) {
          throw new SyntaxException("Invalid use of DISTINCT with function '" + fnName + "'")
        }
        traversal

      case ListComprehension(ExtractScope(_, None, Some(function)), target) =>
        val targetT = walkLocal(target, maybeAlias)
        val functionT = walkLocal(function, maybeAlias)

        val Variable(dependencyName) = function.dependencies.head
        targetT.map(__.unfold().as(dependencyName).flatMap(functionT).fold())

      case PatternComprehension(_, RelationshipsPattern(relationshipChain), maybePredicate, PathExpression(_), _) =>
        val select = __
        val contextWhere = context.copy()
        PatternWalker.walk(contextWhere, select, relationshipChain, maybeAlias)
        maybePredicate.foreach(WhereWalker.walk(contextWhere, select, _))

        val pathName = maybeAlias.getOrElse(context.unsupported("unnamed path comprehension", expression))
        reselectProjection(expression.dependencies.toSeq, context)
          .coalesce(
            select
              .path()
              .from(MATCH_START + pathName),
            __.constant(UNUSED))

      case PatternComprehension(
          _,
          RelationshipsPattern(relationshipChain),
          maybePredicate,
          projection: Expression,
          _) =>
        val traversal = __
        val contextWhere = context.copy()
        PatternWalker.walk(contextWhere, traversal, relationshipChain)
        maybePredicate.foreach(WhereWalker.walk(contextWhere, traversal, _))

        val functionT = walkLocal(projection, maybeAlias)
        if (projection.dependencies.isEmpty) {
          __.map(traversal.flatMap(functionT).fold())
        } else if (projection.dependencies.size == 1) {
          val Variable(dependencyName) = projection.dependencies.head
          __.map(traversal.select(dependencyName).flatMap(functionT).fold())
        } else {
          context.unsupported("pattern comprehension with multiple arguments", expression)
        }

      case PatternExpression(RelationshipsPattern(relationshipChain)) =>
        val traversal = g.start()
        PatternWalker.walk(context, traversal, relationshipChain)
        traversal

      case ListLiteral(expressions @ _ :: _) =>
        asList(expressions: _*)

      case MapExpression(items @ _ :: _) =>
        val keys = items.map(_._1.name)
        val traversal = __.project(keys: _*)
        items.map(_._2).map(walkLocal(_, maybeAlias)).foreach(traversal.by)
        traversal

      case _: Parameter =>
        emptyToNull(
          __.constant(expressionValue(expression, context)),
          context
        )

      case CaseExpression(expr, alternatives, defaultValue) =>
        caseExpression(expr, alternatives, defaultValue)

      case _ =>
        __.constant(expressionValue(expression, context))
    }
  }

  def walkProperty(cypherType: CypherType, key: String, value: Expression): GremlinSteps[T, P] = {
    val traversal = walkLocal(value)
    g.choose(
      g.start().flatMap(traversal).is(p.neq(NULL)).unfold(),
      setProperty(g.start(), cypherType, key, traversal),
      g.start().sideEffect(g.start().properties(key).drop())
    )
  }

  private def typeOf(expr: Expression): CypherType = {
    context.expressionTypes.getOrElse(expr, AnyType.instance)
  }

  private def copy(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    __.flatMap(traversal)
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

    val lhsT = walkLocal(lhs)
    val rhsT = walkLocal(rhs)

    rhsT
      .as(rhsName)
      .flatMap(lhsT)
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
    val lhsT = walkLocal(lhs)
    val rhsT = walkLocal(rhs)
    val rhsName = context.generateName()

    rhsT
      .as(rhsName)
      .flatMap(lhsT)
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
    val rhsName = generateMathName
    val traversal = __.math(s"_ $op $rhsName")
    bothNotNull(lhs, rhs, traversal, rhsName)
  }

  // name limited by MathStep#VARIABLE_PATTERN
  private def generateMathName = context.generateName().replace(" ", "_")

  private def listConcat(lhs: Expression, rhs: Expression) = {
    val rhsName = context.generateName()
    val traversal = __.local(
      __.union(
          __.unfold(),
          __.select(rhsName).unfold()
        )
        .fold()
    )
    bothNotNull(lhs, rhs, traversal, rhsName)
  }

  private def filterElements(args: Seq[Expression], includeNodes: Boolean): GremlinSteps[T, P] = {
    val pathName = args.head match {
      case Variable(name) => name
      case n              => context.unsupported("nodes() or relationships() argument", n)
    }

    if (includeNodes) {
      __.local(__.unfold().where(p.without(PATH_EDGE + pathName)).fold())
    } else {
      __.local(__.unfold().where(p.within(PATH_EDGE + pathName)).fold())
    }
  }

  private def properties(args: Seq[Expression]): GremlinSteps[T, P] = {
    lazy val elementT =
      notNull(
        __.local(
          __.properties()
            .group()
            .by(__.key())
            .by(__.map(__.value()))
        ),
        context)

    val typ = typeOf(args.head)
    typ match {
      case _: NodeType         => elementT
      case _: RelationshipType => elementT
      case _: MapType          => __.identity()
      case _                   => __.map(CustomFunction.cypherProperties())
    }
  }

  private val injectHardLimit = 10000

  private def range(rangeArgs: Seq[Expression]): GremlinSteps[T, P] = {
    def overflow(rangeStart: Any, rangeEnd: Long): Boolean =
      rangeStart match {
        case startVal: Number => (rangeEnd - startVal.longValue()) > injectHardLimit
        case _                => false //unable to validate
      }

    val rangeTraversal = __
    val aggregationTraversal = __.loops()
    val untilCondition = __.loops()

    val rangeLabel = context.generateName()

    val rangeStart = rangeArgs.head match {
      case rangeStart: IntegerLiteral if rangeStart.value < 0 =>
        context.unsupported("negative range start", rangeStart)
      case rangeStart: IntegerLiteral if rangeStart.value == 0 =>
        0L
      case rangeStart: IntegerLiteral =>
        aggregationTraversal.is(p.gte(rangeStart.value))
        rangeStart.value
      case e: Expression =>
        val rangeStartLabel = generateMathName
        rangeTraversal
          .flatMap(walkLocal(e))
          .as(rangeStartLabel)
          .flatMap(runtimeValidation(__.is(p.lt(0)), INVALID_RANGE, context))
        aggregationTraversal.where(p.gte(rangeStartLabel))
        rangeStartLabel
    }

    val rangeEnd = rangeArgs(1) match {
      case rangeEnd: IntegerLiteral if rangeEnd.value < 0 =>
        context.unsupported("negative range end", rangeEnd)
      case rangeEnd: IntegerLiteral if overflow(rangeStart, rangeEnd.value) =>
        context.unsupported(s"Range is too big (must be less than or equal to $injectHardLimit)", rangeEnd)
      case rangeEnd: IntegerLiteral =>
        untilCondition.is(p.gt(rangeEnd.value))
        rangeEnd.value
      case e: Expression =>
        val rangeEndLabel = generateMathName
        rangeTraversal
          .flatMap(walkLocal(e))
          .as(rangeEndLabel)
          .flatMap(runtimeValidation(__.is(p.lt(0)), INVALID_RANGE, context))
        untilCondition.where(p.gt(rangeEndLabel))
        rangeEndLabel
    }

    if (!rangeStart.isInstanceOf[Number] || !rangeEnd.isInstanceOf[Number]) {
      rangeTraversal.flatMap(
        runtimeValidation(__.math(s"$rangeEnd - $rangeStart").is(p.gt(injectHardLimit)), INVALID_RANGE, context))
    }

    if (rangeArgs.size > 2) rangeArgs.last match {
      case rangeStep: IntegerLiteral if rangeStep.value < 0 =>
        context.unsupported("negative range steps", rangeStep)
      case rangeStep: IntegerLiteral if rangeStep.value == 0 =>
        throw new ArgumentException("Step argument to range() cannot be zero")
      case rangeStep: IntegerLiteral =>
        val value = rangeStep.value
        aggregationTraversal.where(__.math(s"(_ - $rangeStart) % $value").is(p.isEq(0)))
      case e: Expression =>
        val rangeStepLabel = generateMathName
        rangeTraversal
          .flatMap(walkLocal(e))
          .as(rangeStepLabel)
          .flatMap(runtimeValidation(__.is(p.lt(0)), INVALID_RANGE, context))
        aggregationTraversal.where(__.math(s"(_ - $rangeStart) % $rangeStepLabel").is(p.isEq(0)))
    }

    rangeTraversal
      .repeat(
        __.sideEffect(
          aggregationTraversal.aggregate(rangeLabel)
        )
      )
      .until(untilCondition)
      .cap(rangeLabel)
  }

  private def size(args: Seq[Expression]): GremlinSteps[T, P] = {
    val typ = typeOf(args.head)
    typ match {
      case ListType(_: PathType) => __.count()
      case _: ListType           => __.count(Scope.local)
      case _                     => __.map(CustomFunction.cypherSize())
    }
  }

  private def caseExpression(
      maybeExpr: Option[Expression],
      alternatives: IndexedSeq[(Expression, Expression)],
      default: Option[Expression]): GremlinSteps[T, P] = {
    val numbersInTokens = alternatives.exists { case (pickToken, _) => pickToken.isInstanceOf[NumberLiteral] }
    val defaultValue = default match {
      case Some(value) => walkLocal(value)
      case None        => __.constant(NULL)
    }

    def nestedChoose(condition: GremlinSteps[T, P]) =
      alternatives.reverse.foldLeft(defaultValue) { (nextOption, alternative) =>
        val (predicate, option) = alternative
        __.choose(walkLocal(predicate).flatMap(condition), walkLocal(option), nextOption)
      }

    def optionChoose(choiceExpr: Expression) = {
      val choose = __.choose(walkLocal(choiceExpr))

      for ((pickToken, option) <- alternatives) {
        choose.option(
          expressionValue(pickToken, context),
          walkLocal(option)
        )
      }

      choose.option(Pick.none, defaultValue)
    }

    maybeExpr match {
      case Some(expr) if numbersInTokens =>
        val name = context.generateName()
        __.flatMap(walkLocal(expr))
          .as(name)
          .flatMap(nestedChoose(__.where(p.isEq(name))))
      case Some(expr) =>
        optionChoose(expr)
      case None =>
        nestedChoose(__.is(p.isEq(true)))
    }
  }
}
