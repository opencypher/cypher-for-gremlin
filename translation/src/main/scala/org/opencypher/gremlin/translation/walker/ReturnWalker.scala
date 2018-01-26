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

import org.apache.tinkerpop.gremlin.structure.Column
import org.neo4j.cypher.internal.frontend.v3_2.ast._
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.walker.NodeUtils.expressionValue
import org.opencypher.gremlin.translation.{Tokens, TranslationBuilder}
import org.opencypher.gremlin.traversal.CustomFunctions

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `RETURN` clause node in the Cypher AST.
  */
object ReturnWalker {
  def walk[T, P](context: StatementContext[T, P], g: TranslationBuilder[T, P], node: Return) {
    new ReturnWalker(context, g).walk(node)
  }
}

private class ReturnWalker[T, P](context: StatementContext[T, P], g: TranslationBuilder[T, P]) {
  case class SubTraversals(
      select: Seq[String],
      pivots: Map[String, TranslationBuilder[T, P]],
      aggregations: Map[String, TranslationBuilder[T, P]])

  sealed trait ReturnFunctionType
  case object Aggregation extends ReturnFunctionType
  case object Pivot extends ReturnFunctionType

  def walk(node: Return): TranslationBuilder[T, P] = {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    val Return(distinct, ReturnItems(_, items), _, skip, limit, _) = node
    val subTraversals = returnSubTraversals(items)
    applyReturnTraversal(node, subTraversals, distinct, skip, limit)
  }

  private def returnSubTraversals(items: Seq[ReturnItem]): SubTraversals = {
    val select = getVariableNames(items)
    val multipleVariables = select.lengthCompare(1) > 0

    val pivotCollector = mutable.LinkedHashMap.empty[String, TranslationBuilder[T, P]]
    val aggregationCollector = mutable.LinkedHashMap.empty[String, TranslationBuilder[T, P]]

    for (item <- items) {
      val AliasedReturnItem(expression, Variable(alias)) = item
      val (returnType, traversal) = pivot(expression, multipleVariables, unfold = false)
      returnType match {
        case Pivot       => pivotCollector.put(alias, traversal)
        case Aggregation => aggregationCollector.put(alias, traversal)
      }
    }

    val pivots = ListMap(pivotCollector.toSeq: _*)
    val aggregations = ListMap(aggregationCollector.toSeq: _*)

    SubTraversals(select, pivots, aggregations)
  }

  private def applyReturnTraversal(
      node: Return,
      subTraversals: SubTraversals,
      distinct: Boolean,
      skip: Option[Skip],
      limit: Option[Limit]): TranslationBuilder[T, P] = {
    val SubTraversals(select, pivots, aggregations) = subTraversals
    val selectIfAny = () => if (select.nonEmpty) g.select(select.toSeq: _*) else g

    if (pivots.nonEmpty && aggregations.nonEmpty) {
      val pivotTraversal = g.start().project(pivots.keySet.toSeq: _*)
      for ((_, expression) <- pivots) pivotTraversal.by(expression)

      val aggregationTraversal = g.start().fold().project(aggregations.keySet.toSeq: _*)
      for ((_, expression) <- aggregations) aggregationTraversal.by(expression)

      selectIfAny()
        .group()
        .by(pivotTraversal)
        .by(aggregationTraversal)

    } else if (pivots.nonEmpty) {
      val pivotTraversal = g.start().project(pivots.keySet.toSeq: _*)
      for ((_, expression) <- pivots) pivotTraversal.by(expression)

      selectIfAny()
        .project(PIVOT)
        .by(pivotTraversal)

    } else if (aggregations.nonEmpty) {
      val aggregationTraversal = g.start().project(aggregations.keySet.toSeq: _*)
      for ((_, expression) <- aggregations) aggregationTraversal.by(expression)

      selectIfAny()
        .fold()
        .project(AGGREGATION)
        .by(aggregationTraversal)
    } else {
      context.unsupported("return clause", node)
    }

    if (distinct) {
      g.dedup()
    }

    for (l <- limit) {
      val Limit(expression) = l
      g.limit(expressionValue(expression, context).asInstanceOf[Long])
    }

    for (s <- skip) {
      val Skip(expression) = s
      val value = expressionValue(expression, context).asInstanceOf[Long]
      if (value != 0L) {
        g.skip(value)
      }
    }

    g.unfold()
  }

  private def getVariableNames(items: Seq[ReturnItem]): Seq[String] = {
    val dependencyNames = for (AliasedReturnItem(expression, _) <- items;
                               Variable(n) <- expression.dependencies) yield n
    dependencyNames.distinct
  }

  private def nullIfNull(
      g: TranslationBuilder[T, P],
      trueChoice: TranslationBuilder[T, P]): TranslationBuilder[T, P] = {
    val p = context.dsl.predicateFactory()
    g.choose(p.neq(Tokens.NULL), trueChoice, g.start().constant(Tokens.NULL))
  }

  private def pivot(
      expression: Expression,
      select: Boolean,
      unfold: Boolean): (ReturnFunctionType, TranslationBuilder[T, P]) = {

    val p = context.dsl.predicateFactory()

    expression match {
      case node: FunctionInvocation =>
        val FunctionInvocation(_, FunctionName(fnName), distinct, args) = node
        val (_, traversal) = pivot(args.head, select, unfold)

        val function = fnName.toLowerCase match {
          case "type"   => nullIfNull(traversal, g.start().label())
          case "labels" => traversal.label().fold()
          case "keys"   => traversal.valueMap().select(Column.keys)
          case "exists" =>
            g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(true), g.start().constant(false))
          case "tostring"      => traversal.map("convertToString", CustomFunctions.convertToString())
          case "tointeger"     => traversal.map("convertToInteger", CustomFunctions.convertToInteger())
          case "tofloat"       => traversal.map("convertToFloat", CustomFunctions.convertToFloat())
          case "toboolean"     => traversal.map("convertToBoolean", CustomFunctions.convertToBoolean())
          case "length"        => traversal.map("length", CustomFunctions.length())
          case "nodes"         => traversal.map("nodes", CustomFunctions.nodes())
          case "relationships" => traversal.map("relationships", CustomFunctions.relationships())
          case "size"          => traversal.map("size", CustomFunctions.size())
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

        (Pivot, traversal.map("listComprehension", CustomFunctions.listComprehension(functionTraversal.current())))
      case PatternComprehension(_, RelationshipsPattern(relationshipChain), maybeExpression, projection, _) =>
        val varName = walkPatternComprehension(relationshipChain, maybeExpression, projection)
        val traversal = g.start().select(varName)

        projection match {
          case PathExpression(_) =>
            (Pivot, traversal.map("pathComprehension", CustomFunctions.pathComprehension()))
          case function: Expression =>
            val (_, functionTraversal) = pivot(function, select, unfold)
            (Pivot, traversal.map("listComprehension", CustomFunctions.listComprehension(functionTraversal.current())))
        }

      case ContainerIndex(expr, idx) =>
        val (_, traversal) = pivot(expr, select, unfold)

        val index = expressionValue(idx, context)
        (Pivot, traversal.map("containerIndex", CustomFunctions.containerIndex(index)))
      case IsNotNull(expr) =>
        val (_, traversal) = pivot(expr, select, unfold)

        (Pivot, g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(true), g.start().constant(false)))
      case IsNull(expr) =>
        val (_, traversal) = pivot(expr, select, unfold)

        (Pivot, g.start().coalesce(traversal.is(p.neq(Tokens.NULL)).constant(false), g.start().constant(true)))
      case node @ (_: Parameter | _: ListLiteral | _: MapExpression | _: Null) =>
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
      subTraversal.as(varName)
    }
    subTraversal
  }

  private def aggregation(expression: Expression, select: Boolean): (ReturnFunctionType, TranslationBuilder[T, P]) = {
    val p = context.dsl.predicateFactory()

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
    val name = context.generateName()

    val select = g.start()
    WhereWalker.walkRelationshipChain(context, select, relationshipChain)
    maybePredicate.foreach(WhereWalker.walk(context, select, _))

    if (projection.isInstanceOf[PathExpression]) {
      select.path()
    }

    g.sideEffect(
      select.aggregate(name)
    )

    name
  }
}
