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
import org.neo4j.cypher.internal.frontend.v3_3.symbols.{NodeType, PathType, RelationshipType}
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}
import org.opencypher.gremlin.traversal.CustomFunction

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `RETURN` clause node in the Cypher AST.
  */
object ProjectionWalker {
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: ProjectionClause) {
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

  private def returnSubTraversals(items: Seq[ReturnItem], finalize: Boolean = false): SubTraversals = {
    val select = getVariableNames(items)
    val multipleVariables = select.lengthCompare(1) > 0

    val pivotCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val aggregationCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val allCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]

    for (item <- items) {
      val AliasedReturnItem(expression, Variable(alias)) = item

      val (_, traversalUnfold) = pivot(alias, expression, multipleVariables, unfold = true, finalize)
      val (returnType, traversal) = pivot(alias, expression, multipleVariables, unfold = false, finalize)

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
    val selectIfAny = () => if (select.nonEmpty) g.select(select: _*) else g

    if (pivots.nonEmpty && aggregations.nonEmpty) {
      val pivotTraversal = getPivotTraversal(pivots)

      val aggregationTraversal = g.start().fold().project(all.keySet.toSeq: _*)
      for ((_, expression) <- all) aggregationTraversal.by(expression)

      selectIfAny()
        .group()
        .by(pivotTraversal)
        .by(aggregationTraversal)
        .unfold()
        .select(Column.values)
    } else if (pivots.nonEmpty) {
      val pivotTraversal = g.start().project(pivots.keySet.toSeq: _*)
      for ((_, expression) <- pivots) pivotTraversal.by(expression)

      selectIfAny()
        .map(pivotTraversal)

    } else if (aggregations.nonEmpty) {
      val aggregationTraversal = g.start().project(aggregations.keySet.toSeq: _*)
      for ((_, expression) <- aggregations) aggregationTraversal.by(expression)

      selectIfAny()
        .fold()
        .map(aggregationTraversal)
    } else {
      g
    }
  }

  private def applyLimits(distinct: Boolean, orderBy: Option[OrderBy], skip: Option[Skip], limit: Option[Limit]) {
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

  private def applyWherePreconditions(items: Seq[ReturnItem]) {
    for (item <- items) {
      val AliasedReturnItem(expression, _) = item
      expression match {
        case _: Variable | _: Property | _: Literal | _: ListLiteral | _: Parameter | _: Null | _: FunctionInvocation |
            _: CountStar | _: PatternComprehension => // Handled separately in #pivot
        case _ =>
          WhereWalker.walk(context, g, expression)
      }
    }
  }

  private def reselectProjection(items: Seq[ReturnItem]) {
    if (items.lengthCompare(1) > 0) {
      g.as(Tokens.TEMP)
    }

    items.toStream.zipWithIndex.foreach {
      case (AliasedReturnItem(_, Variable(alias)), i) => {
        if (i > 0) g.select(Tokens.TEMP)
        g.select(alias).as(alias)
        context.alias(alias)
      }
      case _ =>
    }
  }

  private def getPivotTraversal(pivots: Map[String, GremlinSteps[T, P]]) = {
    if (pivots.size == 1) {
      pivots.values.head
    } else {
      val pivotTraversal = g.start().project(pivots.keySet.toSeq: _*)
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
    g.choose(p.neq(Tokens.NULL), trueChoice, g.start().constant(Tokens.NULL))
  }

  private def pivot(
      alias: String,
      expression: Expression,
      select: Boolean,
      unfold: Boolean,
      finalize: Boolean): (ReturnFunctionType, GremlinSteps[T, P]) = {

    val p = context.dsl.predicates()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node
        val (_, traversal) = pivot(alias, args.head, select, unfold, false)

        val function = fnName.toLowerCase match {
          case "id"     => nullIfNull(traversal, g.start().id())
          case "type"   => nullIfNull(traversal, g.start().label().is(p.neq(Vertex.DEFAULT_LABEL)))
          case "labels" => traversal.label().is(p.neq(Vertex.DEFAULT_LABEL)).fold()
          case "keys"   => traversal.valueMap().select(Column.keys)
          case "exists" =>
            g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(true), g.start().constant(false))
          case "tostring"      => traversal.map(CustomFunction.convertToString())
          case "tointeger"     => traversal.map(CustomFunction.convertToInteger())
          case "tofloat"       => traversal.map(CustomFunction.convertToFloat())
          case "toboolean"     => traversal.map(CustomFunction.convertToBoolean())
          case "length"        => traversal.map(CustomFunction.length())
          case "nodes"         => traversal.map(CustomFunction.nodes())
          case "relationships" => traversal.map(CustomFunction.relationships())
          case "size"          => traversal.map(CustomFunction.size())
          case _ =>
            return aggregation(alias, expression, select)
        }

        if (distinct) {
          throw new SyntaxException("Invalid use of DISTINCT with function '" + fnName + "'")
        }

        (Pivot, function)
      case ListComprehension(ExtractScope(_, _, Some(function)), target) =>
        val (_, traversal) = pivot(alias, target, select, unfold, finalize)
        val (_, functionTraversal) = pivot(alias, function, select, unfold, finalize)

        (Pivot, traversal.map(CustomFunction.listComprehension(functionTraversal.current())))
      case PatternComprehension(_, RelationshipsPattern(relationshipChain), maybeExpression, projection, _) =>
        val varName = walkPatternComprehension(relationshipChain, maybeExpression, projection)
        val traversal = g.start().select(varName)

        projection match {
          case PathExpression(_) =>
            (Pivot, traversal.map(CustomFunction.pathComprehension()))
          case function: Expression =>
            val (_, functionTraversal) = pivot(alias, function, select, unfold, finalize)
            (Pivot, traversal.map(CustomFunction.listComprehension(functionTraversal.current())))
        }

      case ContainerIndex(expr, idx) =>
        val (_, traversal) = pivot(alias, expr, select, unfold, finalize)

        val index = expressionValue(idx, context)
        (Pivot, traversal.map(CustomFunction.containerIndex(index)))
      case IsNotNull(expr) =>
        val (_, traversal) = pivot(alias, expr, select, unfold, finalize)

        (Pivot, g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(true), g.start().constant(false)))
      case IsNull(expr) =>
        val (_, traversal) = pivot(alias, expr, select, unfold, finalize)

        (Pivot, g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(false), g.start().constant(true)))
      case node @ (_: Parameter | _: Literal | _: ListLiteral | _: MapExpression | _: Null) =>
        (Pivot, g.start().constant(expressionValue(node, context)))
      case Property(Variable(varName), PropertyKeyName(keyName: String)) =>
        (
          Pivot,
          nullIfNull(
            baseSelect(varName, select, unfold, only = false),
            g.start()
              .coalesce(
                g.start().values(keyName),
                g.start().constant(Tokens.NULL)
              )
          )
        )
      case Variable(varName) =>
        val value = baseSelect(varName, select, unfold, only = true)
        if (finalize) {
          (Pivot, finalizeValue(value, alias))
        } else {
          (Pivot, value)
        }
      case HasLabels(Variable(varName), List(LabelName(label))) =>
        (
          Pivot,
          nullIfNull(
            baseSelect(varName, select, unfold, only = false),
            g.start()
              .choose(
                g.start().hasLabel(label),
                g.start().constant(true),
                g.start().constant(false)
              )
          )
        )
      case _ => aggregation(alias, expression, select)
    }
  }

  private def baseSelect(varName: String, select: Boolean, unfold: Boolean, only: Boolean) = {
    val subTraversal = g.start()
    if (unfold) {
      subTraversal.unfold()
    }
    if (select) {
      subTraversal.select(varName)
    } else if (only) {
      subTraversal.identity()
    }
    subTraversal
  }

  private def finalizeValue(subTraversal: GremlinSteps[T, P], alias: String) = {
    context.returnTypes.get(alias) match {
      case Some(typ) if typ.isInstanceOf[NodeType] =>
        nullIfNull(subTraversal, g.start().valueMap(true))
      case Some(typ) if typ.isInstanceOf[RelationshipType] =>
        nullIfNull(
          subTraversal,
          g.start()
            .project(ELEMENT, PROJECTION_INV, PROJECTION_OUTV)
            .by(g.start().valueMap(true))
            .by(g.start().inV().id())
            .by(g.start().outV().id())
        )
      case Some(typ) if typ.isInstanceOf[PathType] =>
        subTraversal.map(CustomFunction.finalizePath());
      case _ =>
        subTraversal
    }
  }

  private def aggregation(
      alias: String,
      expression: Expression,
      select: Boolean): (ReturnFunctionType, GremlinSteps[T, P]) = {
    val p = context.dsl.predicates()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node

        val (_, traversal) = pivot(alias, args.head, select, unfold = true, finalize = false)

        if (distinct) {
          traversal.dedup()
        }

        traversal.is(p.neq(Tokens.NULL))

        fnName.toLowerCase match {
          case "count"   => (Aggregation, traversal.count())
          case "collect" => (Aggregation, traversal.fold())
          case "max" =>
            (Aggregation, traversal.max().choose(p.isEq(Integer.MIN_VALUE), g.start().constant(Tokens.NULL)))
          case "min" =>
            (Aggregation, traversal.min().choose(p.isEq(Integer.MAX_VALUE), g.start().constant(Tokens.NULL)))
          case "sum" => (Aggregation, traversal.sum())
          case "avg" => (Aggregation, traversal.mean())
          case _ =>
            throw new SyntaxException(s"Unknown function '$fnName'")
        }
      case CountStar() =>
        (Aggregation, g.start().unfold().count())
      case _: Expression =>
        (Expression, g.start().identity())
      case _ =>
        throw new IllegalArgumentException("Expression not supported: " + expression)
    }
  }

  private def walkPatternComprehension(
      relationshipChain: RelationshipChain,
      maybePredicate: Option[Expression],
      projection: Expression): String = {
    val select = g.start()
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

  private def sort(sortItems: Seq[SortItem]) {
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
          g.by(g.start().select(varName), order)
        case _ =>
          context.unsupported("sort expression", sortItem.expression)
      }
    }
  }
}
