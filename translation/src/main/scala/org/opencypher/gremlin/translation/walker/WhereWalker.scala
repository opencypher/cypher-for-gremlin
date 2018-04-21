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

import java.util

import org.apache.tinkerpop.gremlin.process.traversal.Scope
import org.neo4j.cypher.internal.frontend.v3_3.InputPosition
import org.neo4j.cypher.internal.frontend.v3_3.ast.{BooleanLiteral, _}
import org.neo4j.cypher.internal.frontend.v3_3.symbols.BooleanType
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `WHERE` clause nodes in the Cypher AST.
  */
object WhereWalker {
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Where): Unit = {
    new WhereWalker(context, g).walk(node)
  }

  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], expression: Expression): Unit = {
    new WhereWalker(context, g).walk(expression)
  }

  def walkRelationshipChain[T, P](
      context: StatementContext[T, P],
      g: GremlinSteps[T, P],
      relationshipChain: RelationshipChain) {
    new WhereWalker(context, g).walkRelationshipChain(relationshipChain: RelationshipChain)
  }
}

private class WhereWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  private val freshIds = mutable.HashMap.empty[String, String]

  def walk(node: Where): Unit = {
    node.expression match {
      case _: True => // Ignored
      case _: False =>
        g.limit(0)
      case _: NoneIterablePredicate => // Ignored
      case Ands(ands) =>
        val exprs = ands.filter(!_.isInstanceOf[NoneIterablePredicate])
        if (exprs.nonEmpty) {
          g.where(walkExpression(Ands(exprs)(node.position)))
        }
      case _ =>
        WhereWalker.walk(context, g, node.expression)
    }
  }

  def walk(expression: Expression): Unit = {
    g.where(walkExpression(expression))
  }

  private def walkExpression(node: ASTNode): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    node match {
      case AllIterablePredicate(FilterScope(Variable(freshId), Some(expr)), Variable(varName)) =>
        freshIds(freshId) = varName
        walkExpression(expr)
      case Ands(ands) =>
        g.start().and(ands.map(walkExpression).toSeq: _*)
      case And(lhs, rhs) =>
        g.start().and(walkExpression(lhs), walkExpression(rhs))
      case Ors(ors) =>
        g.start().or(ors.map(walkExpression).toSeq: _*)
      case Or(lhs, rhs) =>
        g.start().or(walkExpression(lhs), walkExpression(rhs))
      case Not(Equals(lhs, rhs)) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.neq))
      case Not(In(lhs, rhs: ListLiteral)) =>
        walkBinaryExpression(lhs, rhs, seqExpressionPredicate(values => p.without(values: _*)))
      case Not(In(lhs, rhs: Parameter)) =>
        walkBinaryExpression(lhs, rhs, seqExpressionPredicate(values => p.without(values: _*)))
      case Not(rhs) =>
        g.start().not(walkExpression(rhs))
      case Equals(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.isEq))
      case LessThan(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.lt))
      case LessThanOrEqual(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.lte))
      case GreaterThan(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.gt))
      case GreaterThanOrEqual(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.gte))
      case StartsWith(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.startsWith))
      case EndsWith(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.endsWith))
      case Contains(lhs, rhs) =>
        walkBinaryExpression(lhs, rhs, expressionPredicate(p.contains))
      case In(lhs, rhs: ListLiteral) =>
        walkBinaryExpression(lhs, rhs, seqExpressionPredicate(values => p.within(values: _*)))
      case In(lhs, rhs: Parameter) =>
        walkBinaryExpression(lhs, rhs, seqExpressionPredicate(values => p.within(values: _*)))
      case expr: RightUnaryOperatorExpression =>
        walkRightUnaryOperatorExpression(expr)
      case expr: FunctionInvocation =>
        walkFunctionInvocation(g.start(), expr)
      case HasLabels(Variable(name), labelNames) =>
        val labels = labelNames.map {
          case LabelName(labelName) => labelName
        }
        g.start().select(name).hasLabel(labels: _*)
      case Parameter(name, _: BooleanType) =>
        g.start().constant(context.parameter(name)).is(p.isEq(true))
      case l: BooleanLiteral =>
        g.start().constant(l.value).is(p.isEq(true))
      case PatternExpression(RelationshipsPattern(relationshipChain)) =>
        val traversal = g.start()
        WhereWalker.walkRelationshipChain(context, traversal, relationshipChain)
        traversal
      case _ =>
        context.unsupported("expression", node)
    }
  }

  private def expressionPredicate(p: AnyRef => P): Expression => P = { expression =>
    p(expressionValue(expression, context))
  }

  private def seqExpressionPredicate(p: Seq[AnyRef] => P): Expression => P = { expression =>
    inlineExpressionValue(expression, context) match {
      case coll: util.Collection[_] => p(coll.asScala.toSeq.asInstanceOf[Seq[AnyRef]])
      case value                    => p(value.asInstanceOf[AnyRef] :: Nil)
    }
  }

  private def walkBinaryExpression(lhs: Expression, rhs: Expression, predicate: Expression => P): GremlinSteps[T, P] = {
    val whereG = g.start()

    rhs match {
      case Variable(v) =>
        whereG.select(v).as(Tokens.TEMP)
      case Property(Variable(varName), PropertyKeyName(keyName)) =>
        whereG.select(varName).values(keyName).as(Tokens.TEMP)
      case function: FunctionInvocation =>
        walkFunctionInvocation(whereG, function).as(Tokens.TEMP)
      case _ =>
    }

    lhs match {
      case Variable(v) =>
        whereG.select(v)
      case Property(Variable(varName), PropertyKeyName(keyName)) =>
        whereG.select(freshIds.getOrElse(varName, varName)).values(keyName)
      case function: FunctionInvocation =>
        walkFunctionInvocation(whereG, function)
      case _ =>
        whereG.constant(expressionValue(lhs, context))
    }

    rhs match {
      case Variable(_) | Property(Variable(_), PropertyKeyName(_)) | _: FunctionInvocation =>
        whereG.where(predicate(StringLiteral(Tokens.TEMP)(InputPosition.NONE)))
      case expression =>
        whereG.is(predicate(expression))
    }

    whereG
  }

  private def walkRightUnaryOperatorExpression(expr: RightUnaryOperatorExpression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    expr match {
      case IsNull(subExpr) =>
        subExpr match {
          case Variable(varName) =>
            g.start().select(varName).is(p.isEq(NULL))
          case Property(Variable(varName), PropertyKeyName(keyName)) =>
            g.start().select(varName).hasNot(keyName)
        }
      case IsNotNull(subExpr) =>
        subExpr match {
          case Variable(varName) =>
            g.start().select(varName).is(p.neq(NULL))
          case Property(Variable(varName), PropertyKeyName(keyName)) =>
            g.start().select(varName).has(keyName)
        }
    }
  }

  private def walkFunctionInvocation(g: GremlinSteps[T, P], function: FunctionInvocation): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    val FunctionInvocation(_, FunctionName(name), _, args) = function
    name.toLowerCase match {
      case "exists" =>
        args.head match {
          case Variable(varName) =>
            g.select(varName).is(p.neq(NULL))
          case Property(Variable(varName), PropertyKeyName(keyName)) =>
            g.select(varName).has(keyName)
        }
      case "length" =>
        args.head match {
          case Variable(varName) =>
            g.select(varName).count(Scope.local)
        }
      case "type" =>
        args.head match {
          case Variable(varName) =>
            g.select(varName).label()
        }
      case _ =>
        context.unsupported("expression", function)
    }
  }

  def walkRelationshipChain(relationshipChain: RelationshipChain): Unit = {
    var firstNode = true
    flattenRelationshipChain(relationshipChain).foreach {
      case NodePattern(variableOption, labelNames, _) =>
        variableOption.foreach {
          case Variable(name) =>
            if (firstNode) {
              g.select(name)
            } else {
              asUniqueName(name, g, context)
            }
        }
        firstNode = false
        if (labelNames.nonEmpty) {
          g.hasLabel(labelNames.head.name)
        }
      case r: RelationshipPattern =>
        RelationshipPatternWalker.walk(None, context, g, r)
      case n =>
        context.unsupported("pattern predicate", n)
    }
  }
}
