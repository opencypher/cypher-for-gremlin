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
object ReturnWalker {
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Return) {
    new ReturnWalker(context, g).walk(node)
  }
}

private class ReturnWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  case class SubTraversals(
      select: Seq[String],
      all: Map[String, GremlinSteps[T, P]],
      pivots: Map[String, GremlinSteps[T, P]],
      aggregations: Map[String, GremlinSteps[T, P]])

  sealed trait ReturnFunctionType

  case object Aggregation extends ReturnFunctionType

  case object Pivot extends ReturnFunctionType

  def walk(node: Return): GremlinSteps[T, P] = {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    val Return(distinct, ReturnItems(_, items), _, _, skip, limit, _) = node
    val subTraversals = returnSubTraversals(items)
    applyReturnTraversal(node, subTraversals, distinct, skip, limit)
  }

  private def returnSubTraversals(items: Seq[ReturnItem]): SubTraversals = {
    val select = getVariableNames(items)
    val multipleVariables = select.lengthCompare(1) > 0

    val pivotCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val aggregationCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]
    val allCollector = mutable.LinkedHashMap.empty[String, GremlinSteps[T, P]]

    for (item <- items) {
      val AliasedReturnItem(expression, Variable(alias)) = item

      val (_, traversalUnfold) = pivot(expression, multipleVariables, unfold = true)
      val (returnType, traversal) = pivot(expression, multipleVariables, unfold = false)

      allCollector.put(alias, traversalUnfold)

      returnType match {
        case Pivot       => pivotCollector.put(alias, traversal)
        case Aggregation => aggregationCollector.put(alias, traversal)
      }
    }

    val pivots = ListMap(pivotCollector.toSeq: _*)
    val aggregations = ListMap(aggregationCollector.toSeq: _*)
    val all = ListMap(allCollector.toSeq: _*)

    SubTraversals(select, all, pivots, aggregations)
  }

  private def applyReturnTraversal(
      node: Return,
      subTraversals: SubTraversals,
      distinct: Boolean,
      skip: Option[Skip],
      limit: Option[Limit]): GremlinSteps[T, P] = {
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
      context.unsupported("return clause", node)
    }

    if (distinct) {
      g.dedup()
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

    g
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
      expression: Expression,
      select: Boolean,
      unfold: Boolean): (ReturnFunctionType, GremlinSteps[T, P]) = {

    val p = context.dsl.predicates()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node
        val (_, traversal) = pivot(args.head, select, unfold)

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
            return aggregation(expression, select)
        }

        if (distinct) {
          throw new SyntaxException("Invalid use of DISTINCT with function '" + fnName + "'")
        }

        (Pivot, function)
      case ListComprehension(ExtractScope(_, _, Some(function)), target) =>
        val (_, traversal) = pivot(target, select, unfold)
        val (_, functionTraversal) = pivot(function, select, unfold)

        (Pivot, traversal.map(CustomFunction.listComprehension(functionTraversal.current())))
      case PatternComprehension(_, RelationshipsPattern(relationshipChain), maybeExpression, projection, _) =>
        val varName = walkPatternComprehension(relationshipChain, maybeExpression, projection)
        val traversal = g.start().select(varName)

        projection match {
          case PathExpression(_) =>
            (Pivot, traversal.map(CustomFunction.pathComprehension()))
          case function: Expression =>
            val (_, functionTraversal) = pivot(function, select, unfold)
            (Pivot, traversal.map(CustomFunction.listComprehension(functionTraversal.current())))
        }

      case ContainerIndex(expr, idx) =>
        val (_, traversal) = pivot(expr, select, unfold)

        val index = expressionValue(idx, context)
        (Pivot, traversal.map(CustomFunction.containerIndex(index)))
      case IsNotNull(expr) =>
        val (_, traversal) = pivot(expr, select, unfold)

        (Pivot, g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(true), g.start().constant(false)))
      case IsNull(expr) =>
        val (_, traversal) = pivot(expr, select, unfold)

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
        (Pivot, baseSelect(varName, select, unfold, only = true))
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
      case _ => aggregation(expression, select)
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

  private def aggregation(expression: Expression, select: Boolean): (ReturnFunctionType, GremlinSteps[T, P]) = {
    val p = context.dsl.predicates()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node

        val (_, traversal) = pivot(args.head, select, unfold = true)

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
}
