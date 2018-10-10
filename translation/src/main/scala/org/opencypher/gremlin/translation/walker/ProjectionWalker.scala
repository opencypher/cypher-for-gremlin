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
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.symbols._

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `RETURN` clause node in the Cypher AST.
  */
object ProjectionWalker {
  def walk[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: ProjectionClause): Unit = {
    node match {
      case Return(distinct, ReturnItems(_, items), orderBy, skip, limit, _) =>
        new ProjectionWalker(context, g).walkFinal(distinct, items, orderBy, skip, limit)
      case With(distinct, ReturnItems(_, items), orderBy, skip, limit, where) =>
        new ProjectionWalker(context, g).walkIntermediate(distinct, items, orderBy, skip, limit, where)
      case _ => context.unsupported("projection", node)
    }
  }
}

private class ProjectionWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

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
      limit: Option[Limit]): Unit = {
    ensureFirstStatement(g, context)

    val subTraversals = returnSubTraversals(items)

    applyProjection(subTraversals)
    applyLimits(distinct, orderBy, skip, limit)
  }

  def walkIntermediate(
      distinct: Boolean,
      items: Seq[ReturnItem],
      orderBy: Option[OrderBy],
      skip: Option[Skip],
      limit: Option[Limit],
      where: Option[Where]): Unit = {
    applyWhereFromReturnItems(items)
    walk(distinct, items, orderBy, skip, limit)
    applyWhere(where)
    reselectProjection(items)
  }

  def walkFinal(
      distinct: Boolean,
      items: Seq[ReturnItem],
      orderBy: Option[OrderBy],
      skip: Option[Skip],
      limit: Option[Limit]): Unit = {
    walk(distinct, items, orderBy, skip, limit)
    finalizeProjection(items)
  }

  private def __ = {
    g.start()
  }

  private def returnSubTraversals(items: Seq[ReturnItem]): SubTraversals = {
    val select = getVariableNames(items)

    val pivotCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val aggregationCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val allCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]

    for (item <- items) {
      val AliasedReturnItem(expression, Variable(alias)) = item

      val (returnType, traversal) = subTraversal(alias, expression)

      allCollector.put(alias, traversal)

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
    lazy val selectMap = {
      if (select.isEmpty) {
        g
      } else if (select.lengthCompare(1) == 0) {
        g.as(UNUSED).select(select.head, UNUSED)
      } else {
        g.select(select: _*)
      }
    }

    if (pivots.nonEmpty && aggregations.nonEmpty) {
      val pivotTraversal = if (pivots.size == 1) {
        pivots.values.head
      } else {
        val traversal = __.project(pivots.keySet.toSeq: _*)
        for ((_, expression) <- pivots) traversal.by(expression)
        traversal
      }

      val aggregationTraversal = __.fold().project(all.keySet.toSeq: _*)
      for ((_, expression) <- all) aggregationTraversal.by(__.unfold().flatMap(expression))

      selectMap
        .group()
        .by(pivotTraversal)
        .by(aggregationTraversal)
        .unfold()
        .select(Column.values)
    } else if (pivots.nonEmpty) {
      val pivotTraversal = __.project(pivots.keySet.toSeq: _*)
      for ((_, expression) <- pivots) pivotTraversal.by(expression)

      selectMap
        .flatMap(pivotTraversal)

    } else if (aggregations.nonEmpty) {
      val aggregationTraversal = __.project(aggregations.keySet.toSeq: _*)
      for ((_, expression) <- aggregations) aggregationTraversal.by(__.unfold().flatMap(expression))

      selectMap
        .fold()
        .flatMap(aggregationTraversal)
    } else {
      g
    }
  }

  private def applyLimits(
      distinct: Boolean,
      orderBy: Option[OrderBy],
      skip: Option[Skip],
      limit: Option[Limit]): Unit = {
    def roundedLongValue(expression: Expression) =
      Math.round(inlineExpressionValue(expression, context, classOf[Number]).doubleValue())

    if (distinct) {
      g.dedup()
    }

    orderBy match {
      case Some(OrderBy(sortItems)) => sort(sortItems)
      case _                        =>
    }

    for (Skip(expression) <- skip) {
      val value = roundedLongValue(expression)
      if (value != 0L) {
        g.skip(value)
      }
    }

    for (Limit(expression) <- limit) {
      val value = roundedLongValue(expression)
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

  private def applyWhereFromReturnItems(items: Seq[ReturnItem]): Unit = {
    items.map {
      case AliasedReturnItem(expression, _)   => expression
      case UnaliasedReturnItem(expression, _) => expression
    }.filter(isWherePrecondition)
      .foreach(WhereWalker.walk(context, g, _))
  }

  private def applyWhere(where: Option[Where]): Unit = {
    where.foreach(WhereWalker.walk(context, g, _))
  }

  private def reselectProjection(items: Seq[ReturnItem]): Unit = {
    val variables = items.flatMap {
      case AliasedReturnItem(_, variable) => Some(variable)
      case _                              => None
    }
    g.flatMap(NodeUtils.reselectProjection(variables, context))
  }

  private def finalizeProjection(items: Seq[ReturnItem]): Unit = {
    val aliasToExpression = items.flatMap {
      case AliasedReturnItem(expression, Variable(alias)) => Some((alias, expression))
      case _                                              => None
    }

    val needsFinalization = aliasToExpression.exists(n =>
      qualifiedType(n._2) match {
        case (_: NodeType, _)                   => true
        case (_: ListType, _: NodeType)         => true
        case (_: RelationshipType, _)           => true
        case (_: ListType, _: RelationshipType) => true
        case (_: PathType, _)                   => true
        case (_: ListType, _: PathType)         => true
        case _                                  => false
    })

    if (needsFinalization) {
      g.project(aliasToExpression.map(_._1): _*)
      for ((alias, expression) <- aliasToExpression) {
        g.by(finalizeValue(__.select(alias), alias, expression))
      }
    }
  }

  private def getVariableNames(items: Seq[ReturnItem]): Seq[String] = {
    val dependencyNames = for (AliasedReturnItem(expression, _) <- items;
                               Variable(n) <- expression.dependencies) yield n
    dependencyNames.distinct
  }

  private def subTraversal(alias: String, expression: Expression): (ReturnFunctionType, GremlinSteps[T, P]) = {
    if (expression.containsAggregate) {
      aggregation(alias, expression)
    } else {
      (Pivot, walkLocal(expression, Some(alias)))
    }
  }

  private def finalizeValue(
      subTraversal: GremlinSteps[T, P],
      variable: String,
      expression: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()

    lazy val finalizeNode =
      __.valueMap(true)

    lazy val finalizeRelationship =
      __.project(PROJECTION_ELEMENT, PROJECTION_INV, PROJECTION_OUTV)
        .by(__.valueMap(true))
        .by(__.inV().id())
        .by(__.outV().id())

    lazy val finalizePath =
      __.is(p.neq(UNUSED))
        .project(PROJECTION_RELATIONSHIP, PROJECTION_ELEMENT)
        .by(
          __.select(PATH_EDGE + variable)
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

    def notNull(traversal: GremlinSteps[T, P]): GremlinSteps[T, P] = {
      NodeUtils.notNull(traversal, context)
    }

    qualifiedType(expression) match {
      case (_: NodeType, _) =>
        subTraversal.flatMap(notNull(finalizeNode))
      case (_: ListType, _: NodeType) =>
        __.flatMap(subTraversal)
          .local(
            __.unfold()
              .is(p.neq(NULL))
              .flatMap(finalizeNode)
              .fold()
          )
      case (_: RelationshipType, _) =>
        subTraversal.flatMap(notNull(finalizeRelationship))
      case (_: ListType, _: RelationshipType) =>
        subTraversal.flatMap(
          notNull(
            __.local(
              __.unfold()
                .flatMap(finalizeRelationship)
                .fold())))
      case (_: PathType, _) =>
        subTraversal.flatMap(notNull(finalizePath))
      case (_: ListType, _: PathType) =>
        subTraversal.flatMap(
          notNull(
            __.flatMap(finalizePath)
              .fold()))
      case _ =>
        subTraversal
    }
  }

  private def qualifiedType(expression: Expression): (CypherType, CypherType) =
    context.expressionTypes.get(expression) match {
      case Some(typ: ListType) => (typ, typ.innerType)
      case Some(typ)           => (typ, AnyType.instance)
      case _                   => (AnyType.instance, AnyType.instance)
    }

  private def aggregation(alias: String, expression: Expression): (ReturnFunctionType, GremlinSteps[T, P]) = {
    val p = context.dsl.predicates()

    expression match {
      case FunctionInvocation(_, FunctionName(fnName), distinct, args) =>
        if (args.flatMap(n => n +: n.subExpressions).exists {
              case FunctionInvocation(_, FunctionName("rand"), _, _) => true
              case _                                                 => false
            }) throw new SyntaxException("Can't use non-deterministic (random) functions inside of aggregate functions")

        if (args.exists(_.containsAggregate))
          throw new SyntaxException("Can't use aggregate functions inside of aggregate functions")

        val (_, traversal) = subTraversal(alias, args.head)

        if (distinct) {
          traversal.dedup()
        }

        traversal.is(p.neq(NULL))

        fnName.toLowerCase match {
          case "avg" =>
            (Aggregation, traversal.mean())
          case "collect" =>
            (Aggregation, traversal.fold())
          case "count" =>
            (Aggregation, traversal.count())
          case "max" =>
            (Aggregation, traversal.max())
          case "min" =>
            (Aggregation, traversal.min())
          case "percentilecont" =>
            (Aggregation, aggregateWithArguments(args, alias).map(CustomFunction.cypherPercentileCont()))
          case "percentiledisc" =>
            (Aggregation, aggregateWithArguments(args, alias).map(CustomFunction.cypherPercentileDisc()))
          case "sum" =>
            (Aggregation, traversal.sum())
          case _ =>
            throw new SyntaxException(s"Unknown function '$fnName'")
        }
      case CountStar() =>
        (Aggregation, __.count())
      case _: Expression if isWherePrecondition(expression) =>
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
      val sortExpression = walkLocal(sortItem.expression, None)
      g.by(sortExpression, order)
    }
  }

  private def aggregateWithArguments(args: Seq[Expression], alias: String): GremlinSteps[T, P] = {
    val keys = args.map(_ => context.generateName())
    val traversal = walkLocal(args.head, Some(alias)).fold().project(keys: _*).by(__.identity())
    args.drop(1).map(walkLocal(_, Some(alias))).foreach(traversal.by)
    traversal.select(Column.values)
  }

  private def walkLocal(expression: Expression, maybeAlias: Option[String]): GremlinSteps[T, P] = {
    ExpressionWalker.walkLocal(context, g, expression, maybeAlias)
  }
}
