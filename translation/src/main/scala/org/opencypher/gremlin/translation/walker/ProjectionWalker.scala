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
import org.apache.tinkerpop.gremlin.structure.Column
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.symbols._

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.util.Try

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
      finalize: Boolean): Unit = {
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
      limit: Option[Limit]): Unit = {

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
      case _: Add | _: ContainerIndex | _: CountStar | _: Divide | _: FunctionInvocation | _: ListLiteral | _: Literal |
          _: MapExpression | _: Modulo | _: Multiply | _: Null | _: Parameter | _: PatternComprehension | _: Pow |
          _: Property | _: Subtract | _: Variable =>
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
    val name = context.generateName()
    if (items.lengthCompare(1) > 0) {
      g.as(name)
    }

    items.toStream.zipWithIndex.foreach {
      case (AliasedReturnItem(_, Variable(alias)), i) =>
        if (i > 0) g.select(name)
        g.select(alias).as(alias)
        context.alias(alias)
      case _ =>
    }
  }

  private def getPivotTraversal(pivots: Map[String, GremlinSteps[T, P]]): GremlinSteps[T, P] = {
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
    Try(ExpressionWalker.walkLocal(context, g, expression)).map { localTraversal =>
      if (finalize && expression.isInstanceOf[Variable]) {
        (Pivot, finalizeValue(localTraversal, alias))
      } else {
        (Pivot, localTraversal)
      }
    }.recover {
      case e: SyntaxException if e.getMessage.startsWith("Unknown function") =>
        aggregation(alias, expression, finalize)
      case e: UnsupportedOperationException if e.getMessage.startsWith("Unsupported value expression") =>
        aggregation(alias, expression, finalize)
    }.get
  }

  private def finalizeValue(subTraversal: GremlinSteps[T, P], alias: String): GremlinSteps[T, P] = {
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
            (Aggregation, traversal.max())
          case "min" =>
            (Aggregation, traversal.min())
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
