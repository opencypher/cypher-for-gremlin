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

import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.structure.{Column, Vertex}
import org.neo4j.cypher.internal.frontend.v3_3.ast._
import org.neo4j.cypher.internal.frontend.v3_3.symbols.{CypherType, ListType, NodeType, PathType, RelationshipType}
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `RETURN` clause node in the Cypher AST.
  */
object ProjectionWalker {
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: ProjectionClause): Unit = {
    node match {
      case Return(distinct, ReturnItems(_, items), _, orderBy, skip, limit, _) =>
        new ProjectionWalker(context, g).walk(distinct, items, orderBy, skip, limit, finalize = true)
      case With(distinct, ReturnItems(_, items), _, orderBy, skip, limit, _) =>
        new ProjectionWalker(context, g).walkIntermediate(distinct, items, orderBy, skip, limit)
      case _ => context.unsupported("projection", node)
    }
  }
}

private class ProjectionWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  case class SubTraversals(
      select: Seq[String],
      all: Map[String, GremlinSteps[T, P]],
      pivots: Map[String, GremlinSteps[T, P]],
      aggregations: Map[String, GremlinSteps[T, P]])

  sealed trait ReturnFunctionType

  case object Aggregation extends ReturnFunctionType

  case object Expression extends ReturnFunctionType

  case object Pivot extends ReturnFunctionType

  def walk(
      distinct: Boolean,
      items: Seq[ReturnItem],
      orderBy: Option[OrderBy],
      skip: Option[Skip],
      limit: Option[Limit],
      finalize: Boolean) {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    val subTraversals = returnSubTraversals(items, finalize)

    applyProjection(subTraversals)
    applyLimits(distinct, orderBy, skip, limit)
  }

  def walkIntermediate(
      distinct: Boolean,
      items: Seq[ReturnItem],
      orderBy: Option[OrderBy],
      skip: Option[Skip],
      limit: Option[Limit]) {

    applyWherePreconditions(items)
    walk(distinct, items, orderBy, skip, limit, finalize = false)
    reselectProjection(items)
  }

  private def __ = {
    g.start()
  }

  private def returnSubTraversals(items: Seq[ReturnItem], finalize: Boolean = false): SubTraversals = {
    val select = getVariableNames(items)

    val pivotCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val aggregationCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val allCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]

    for (item <- items) {
      val AliasedReturnItem(expression, Variable(alias)) = item

      val (_, traversalUnfold) = pivot(alias, expression, finalize)
      val (returnType, traversal) = pivot(alias, expression, finalize)

      allCollector.put(alias, traversalUnfold)

      returnType match {
        case Pivot       => pivotCollector.put(alias, traversal)
        case Aggregation => aggregationCollector.put(alias, traversal)
        case Expression  => aggregationCollector.put(alias, traversal)
      }
    }

    val pivots = ListMap(pivotCollector.toSeq: _*)
    val aggregations = ListMap(aggregationCollector.toSeq: _*)
    val all = ListMap(allCollector.toSeq: _*)

    SubTraversals(select, all, pivots, aggregations)
  }

  private def applyProjection(subTraversals: SubTraversals): GremlinSteps[T, P] = {
    val SubTraversals(select, all, pivots, aggregations) = subTraversals
    val selectMap = () =>
      if (select.isEmpty) {
        g
      } else if (select.lengthCompare(1) == 0) {
        g.as(UNUSED).select(select.head, UNUSED)
      } else {
        g.select(select: _*)
    }

    if (pivots.nonEmpty && aggregations.nonEmpty) {
      val pivotTraversal = getPivotTraversal(pivots)

      val aggregationTraversal = __.fold().project(all.keySet.toSeq: _*)
      for ((_, expression) <- all) aggregationTraversal.by(__.unfold().map(expression))

      selectMap()
        .group()
        .by(pivotTraversal)
        .by(aggregationTraversal)
        .unfold()
        .select(Column.values)
    } else if (pivots.nonEmpty) {
      val pivotTraversal = __.project(pivots.keySet.toSeq: _*)
      for ((_, expression) <- pivots) pivotTraversal.by(expression)

      selectMap()
        .map(pivotTraversal)

    } else if (aggregations.nonEmpty) {
      val aggregationTraversal = __.project(aggregations.keySet.toSeq: _*)
      for ((_, expression) <- aggregations) aggregationTraversal.by(__.unfold().map(expression))

      selectMap()
        .fold()
        .map(aggregationTraversal)
    } else {
      g
    }
  }

  private def applyLimits(
      distinct: Boolean,
      orderBy: Option[OrderBy],
      skip: Option[Skip],
      limit: Option[Limit]): Unit = {
    if (distinct) {
      g.dedup()
    }

    orderBy match {
      case Some(OrderBy(sortItems)) => sort(sortItems)
      case _                        =>
    }

    for (s <- skip) {
      val Skip(expression) = s
      val value = inlineExpressionValue(expression, context, classOf[Number]).longValue()
      if (value != 0L) {
        g.skip(value)
      }
    }

    for (l <- limit) {
      val Limit(expression) = l
      val value = inlineExpressionValue(expression, context, classOf[Number]).longValue()
      g.limit(value)
    }
  }

  private def isWherePrecondition(expression: Expression): Boolean = {
    expression match {
      case _: Variable | _: Property | _: Literal | _: ListLiteral | _: Parameter | _: Null | _: FunctionInvocation |
          _: CountStar | _: PatternComprehension | _: Add | _: Subtract | _: Multiply | _: Divide | _: Pow |
          _: Modulo =>
        false
      case _ =>
        true
    }
  }

  private def applyWherePreconditions(items: Seq[ReturnItem]): Unit = {
    items.map {
      case AliasedReturnItem(expression, _)   => expression
      case UnaliasedReturnItem(expression, _) => expression
    }.filter(isWherePrecondition)
      .foreach(WhereWalker.walk(context, g, _))
  }

  private def reselectProjection(items: Seq[ReturnItem]): Unit = {
    if (items.lengthCompare(1) > 0) {
      g.as(TEMP)
    }

    items.toStream.zipWithIndex.foreach {
      case (AliasedReturnItem(_, Variable(alias)), i) =>
        if (i > 0) g.select(TEMP)
        g.select(alias).as(alias)
        context.alias(alias)
      case _ =>
    }
  }

  private def getPivotTraversal(pivots: Map[String, GremlinSteps[T, P]]) = {
    if (pivots.size == 1) {
      pivots.values.head
    } else {
      val pivotTraversal = __.project(pivots.keySet.toSeq: _*)
      for ((_, expression) <- pivots) pivotTraversal.by(expression)
      pivotTraversal
    }
  }

  private def getVariableNames(items: Seq[ReturnItem]): Seq[String] = {
    val dependencyNames = for (AliasedReturnItem(expression, _) <- items;
                               Variable(n) <- expression.dependencies) yield n
    dependencyNames.distinct
  }

  private def nullIfNull(g: GremlinSteps[T, P], trueChoice: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    g.choose(p.neq(NULL), trueChoice, g.start().constant(NULL))
  }

  private def pivot(
      alias: String,
      expression: Expression,
      finalize: Boolean): (ReturnFunctionType, GremlinSteps[T, P]) = {

    val p = context.dsl.predicates()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node
        val traversals = args.map(pivot(alias, _, finalize = false)._2)

        val function = fnName.toLowerCase match {
          case "abs"           => traversals.head.math("abs(_)")
          case "exists"        => __.coalesce(traversals.head.is(p.neq(NULL)).constant(true), __.constant(false))
          case "coalesce"      => __.coalesce(traversals.init.map(_.is(p.neq(NULL))) :+ traversals.last: _*)
          case "id"            => nullIfNull(traversals.head, __.id())
          case "keys"          => traversals.head.valueMap().select(Column.keys)
          case "labels"        => traversals.head.label().is(p.neq(Vertex.DEFAULT_LABEL)).fold()
          case "length"        => traversals.head.map(CustomFunction.length())
          case "nodes"         => traversals.head.map(CustomFunction.nodes())
          case "properties"    => nullIfNull(traversals.head, __.map(CustomFunction.properties()))
          case "relationships" => traversals.head.map(CustomFunction.relationships())
          case "size"          => traversals.head.map(CustomFunction.size())
          case "sqrt"          => traversals.head.math("sqrt(_)")
          case "type"          => nullIfNull(traversals.head, __.label().is(p.neq(Vertex.DEFAULT_LABEL)))
          case "toboolean"     => traversals.head.map(CustomFunction.convertToBoolean())
          case "tofloat"       => traversals.head.map(CustomFunction.convertToFloat())
          case "tointeger"     => traversals.head.map(CustomFunction.convertToIntegerType())
          case "tostring"      => traversals.head.map(CustomFunction.convertToString())
          case _ =>
            return aggregation(alias, expression, finalize)
        }

        if (distinct) {
          throw new SyntaxException("Invalid use of DISTINCT with function '" + fnName + "'")
        }

        (Pivot, function)
      case ListComprehension(ExtractScope(_, _, Some(function)), target) if function.dependencies.size == 1 =>
        val (_, traversal) = pivot(alias, target, finalize)
        val (_, functionTraversal) = pivot(alias, function, finalize)

        val Variable(dependencyName) = function.dependencies.head
        (Pivot, traversal.unfold().as(dependencyName).map(functionTraversal).fold())
      case PatternComprehension(_, RelationshipsPattern(relationshipChain), maybeExpression, projection, _) =>
        val varName = walkPatternComprehension(relationshipChain, maybeExpression, projection)
        val traversal = __.select(varName)

        projection match {
          case PathExpression(_) =>
            (Pivot, traversal.map(CustomFunction.pathComprehension()))
          case expression: Expression =>
            val (_, functionTraversal) = pivot(alias, expression, finalize)
            if (expression.dependencies.isEmpty) {
              (Pivot, traversal.unfold().map(functionTraversal).fold())
            } else if (expression.dependencies.size == 1) {
              val Variable(dependencyName) = expression.dependencies.head
              (Pivot, traversal.unfold().as(dependencyName).map(functionTraversal).fold())
            } else {
              context.unsupported("pattern comprehension with multiple arguments", expression)
            }
        }

      case ContainerIndex(expr, idx) =>
        val (_, traversal) = pivot(alias, expr, finalize)

        val index = expressionValue(idx, context)
        (Pivot, traversal.map(CustomFunction.containerIndex(index)))
      case IsNotNull(expr) =>
        val (_, traversal) = pivot(alias, expr, finalize)

        (Pivot, __.coalesce(traversal.is(p.neq(NULL)).constant(true), __.constant(false)))
      case IsNull(expr) =>
        val (_, traversal) = pivot(alias, expr, finalize)

        (Pivot, __.coalesce(traversal.is(p.neq(NULL)).constant(false), __.constant(true)))
      case node @ (_: Parameter | _: Literal | _: ListLiteral | _: MapExpression | _: Null) =>
        (Pivot, __.constant(expressionValue(node, context)))
      case Property(Variable(varName), PropertyKeyName(keyName: String)) =>
        (
          Pivot,
          nullIfNull(
            __.select(varName),
            __.coalesce(
              __.values(keyName),
              __.constant(NULL)
            )
          )
        )
      case Variable(varName) =>
        val value = __.select(varName)
        if (finalize) {
          (Pivot, finalizeValue(value, alias))
        } else {
          (Pivot, value)
        }
      case HasLabels(Variable(varName), List(LabelName(label))) =>
        (
          Pivot,
          nullIfNull(
            __.select(varName),
            __.choose(
              __.hasLabel(label),
              __.constant(true),
              __.constant(false)
            )
          )
        )

      case Add(e1, e2)      => math(alias, finalize, e1, e2, "+")
      case Subtract(e1, e2) => math(alias, finalize, e1, e2, "-")
      case Multiply(e1, e2) => math(alias, finalize, e1, e2, "*")
      case Divide(e1, e2)   => math(alias, finalize, e1, e2, "/")
      case Pow(e1, e2)      => math(alias, finalize, e1, e2, "^")
      case Modulo(e1, e2)   => math(alias, finalize, e1, e2, "%")

      case Not(rhs) =>
        val rhsT = pivot(alias, rhs, finalize = false)._2
        (
          Pivot,
          __.choose(
            __.map(rhsT).is(p.isEq(NULL)),
            __.constant(NULL),
            __.choose(
              __.map(rhsT).is(p.isEq(true)),
              __.constant(false),
              __.constant(true)
            )
          )
        )
      case Ands(ands) =>
        val traversals = ands.map(pivot(alias, _, finalize = false)._2).toSeq
        (
          Pivot,
          __.choose(
            __.and(traversals.map(__.map(_)).map(_.is(p.isEq(true))): _*),
            __.constant(true),
            __.choose(
              __.or(traversals.map(__.map(_)).map(_.is(p.isEq(false))): _*),
              __.constant(false),
              __.constant(NULL)
            )
          )
        )
      case Ors(ors) =>
        val traversals = ors.map(pivot(alias, _, finalize = false)._2).toSeq
        (
          Pivot,
          __.choose(
            __.or(traversals.map(__.map(_)).map(_.is(p.isEq(true))): _*),
            __.constant(true),
            __.choose(
              __.and(traversals.map(__.map(_)).map(_.is(p.isEq(false))): _*),
              __.constant(false),
              __.constant(NULL)
            )
          )
        )
      case Xor(lhs, rhs) =>
        val lhsT = pivot(alias, lhs, finalize = false)._2
        val rhsT = pivot(alias, rhs, finalize = false)._2
        (
          Pivot,
          __.choose(
            __.or(__.map(lhsT).is(p.isEq(NULL)), __.map(rhsT).is(p.isEq(NULL))),
            __.constant(NULL),
            __.choose(
              __.map(rhsT).as(TEMP).map(lhsT).where(p.neq(TEMP)),
              __.constant(true),
              __.constant(false)
            )
          )
        )

      case _ => aggregation(alias, expression, finalize)
    }
  }

  private def finalizeValue(subTraversal: GremlinSteps[T, P], alias: String) = {
    def hasInnerType(typ: CypherType, expected: CypherType): Boolean =
      typ.isInstanceOf[ListType] && typ.asInstanceOf[ListType].innerType == expected

    val p = context.dsl.predicates()

    context.returnTypes.get(alias) match {
      case Some(typ) if typ.isInstanceOf[NodeType] =>
        nullIfNull(subTraversal, __.valueMap(true))
      case Some(typ) if typ.isInstanceOf[ListType] && hasInnerType(typ, NodeType.instance) =>
        __.map(subTraversal).unfold().is(p.neq(NULL)).valueMap(true).fold()
      case Some(typ) if typ.isInstanceOf[RelationshipType] =>
        nullIfNull(
          subTraversal,
          __.project(PROJECTION_ELEMENT, PROJECTION_INV, PROJECTION_OUTV)
            .by(__.valueMap(true))
            .by(__.inV().id())
            .by(__.outV().id())
        )
      case Some(typ) if typ.isInstanceOf[ListType] && hasInnerType(typ, RelationshipType.instance) =>
        __.map(subTraversal)
          .unfold()
          .is(p.neq(NULL))
          .project(PROJECTION_ELEMENT, PROJECTION_INV, PROJECTION_OUTV)
          .by(__.valueMap(true))
          .by(__.inV().id())
          .by(__.outV().id())
          .fold()
      case Some(typ) if typ.isInstanceOf[PathType] =>
        nullIfNull(
          subTraversal,
          __.project(PROJECTION_RELATIONSHIP, PROJECTION_ELEMENT)
            .by(
              __.select(PATH_EDGE + alias)
                .unfold()
                .project(PROJECTION_ID, PROJECTION_INV, PROJECTION_OUTV)
                .by(__.id())
                .by(__.inV().id())
                .by(__.outV().id())
                .fold()
            )
            .by(
              __.unfold()
                .is(p.neq(START))
                .valueMap(true)
                .fold())
        )
      case _ =>
        subTraversal
    }
  }

  private def aggregation(
      alias: String,
      expression: Expression,
      finalize: Boolean): (ReturnFunctionType, GremlinSteps[T, P]) = {
    val p = context.dsl.predicates()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node

        val (_, traversal) = pivot(alias, args.head, finalize = false)

        if (distinct) {
          traversal.dedup()
        }

        traversal.is(p.neq(NULL))

        fnName.toLowerCase match {
          case "avg" =>
            (Aggregation, traversal.mean())
          case "collect" if finalize =>
            (Aggregation, finalizeValue(traversal.fold(), alias))
          case "collect" =>
            (Aggregation, traversal.fold())
          case "count" =>
            (Aggregation, traversal.count())
          case "max" =>
            (Aggregation, traversal.max().choose(p.isEq(Integer.MIN_VALUE), __.constant(NULL)))
          case "min" =>
            (Aggregation, traversal.min().choose(p.isEq(Integer.MAX_VALUE), __.constant(NULL)))
          case "percentilecont" =>
            val percentile = inlineExpressionValue(args(1), context, classOf[java.lang.Number]).doubleValue()
            (Aggregation, traversal.fold().map(CustomFunction.percentileCont(percentile)))
          case "percentiledisc" =>
            val percentile = inlineExpressionValue(args(1), context, classOf[java.lang.Number]).doubleValue()
            (Aggregation, traversal.fold().map(CustomFunction.percentileDisc(percentile)))
          case "sum" =>
            (Aggregation, traversal.sum())
          case _ =>
            throw new SyntaxException(s"Unknown function '$fnName'")
        }
      case CountStar() =>
        (Aggregation, __.count())
      case _: Expression if !finalize && isWherePrecondition(expression) =>
        (Expression, __.identity())
      case _ =>
        context.unsupported("expression", expression)
    }
  }

  private def math(alias: String, finalize: Boolean, e1: Expression, e2: Expression, op: String) = {
    val p = context.dsl.predicates()

    val (_, traversal1) = pivot(alias, e1, finalize)
    val (_, traversal2) = pivot(alias, e2, finalize)

    val lhsName = context.generateName().replace(" ", "_") // name limited by MathStep#VARIABLE_PATTERN

    (
      Pivot,
      __.map(traversal1)
        .as(lhsName)
        .map(traversal2)
        .choose(
          __.or(__.is(p.isEq(NULL)), __.select(lhsName).is(p.isEq(NULL))),
          __.constant(NULL),
          __.math(s"$lhsName $op _")))
  }

  private def walkPatternComprehension(
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

  private def sort(sortItems: Seq[SortItem]): Unit = {
    g.order()
    for (sortItem <- sortItems) {
      val order = sortItem match {
        case _: AscSortItem =>
          Order.incr
        case _: DescSortItem =>
          Order.decr
      }
      sortItem.expression match {
        case Variable(varName) =>
          g.by(__.select(varName), order)
        case _ =>
          context.unsupported("sort expression", sortItem.expression)
      }
    }
  }
}
