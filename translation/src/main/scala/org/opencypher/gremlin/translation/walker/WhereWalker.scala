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
import org.neo4j.cypher.internal.frontend.v3_2.ast.{BooleanLiteral, _}
import org.neo4j.cypher.internal.frontend.v3_2.symbols.{BooleanType, ListType}
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
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Where) {
    new WhereWalker(context, g).walk(node)
  }

  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], expression: Expression) {
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

  def walk(node: Where) {
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

  def walk(expression: Expression) {
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
        walkBinaryExpression(lhs, p.neq(walkRhs(rhs)))
      case Not(In(lhs, ListLiteral(list))) =>
        walkBinaryExpression(lhs, p.without(list.map(walkRhs): _*))
      case Not(rhs) =>
        g.start().not(walkExpression(rhs))
      case Equals(lhs, rhs) =>
        walkBinaryExpression(lhs, p.isEq(walkRhs(rhs)))
      case LessThan(lhs, rhs) =>
        walkBinaryExpression(lhs, p.lt(walkRhs(rhs)))
      case LessThanOrEqual(lhs, rhs) =>
        walkBinaryExpression(lhs, p.lte(walkRhs(rhs)))
      case GreaterThan(lhs, rhs) =>
        walkBinaryExpression(lhs, p.gt(walkRhs(rhs)))
      case GreaterThanOrEqual(lhs, rhs) =>
        walkBinaryExpression(lhs, p.gte(walkRhs(rhs)))
      case StartsWith(lhs, rhs) =>
        walkBinaryExpression(lhs, p.startsWith(walkRhs(rhs)))
      case EndsWith(lhs, rhs) =>
        walkBinaryExpression(lhs, p.endsWith(walkRhs(rhs)))
      case Contains(lhs, rhs) =>
        walkBinaryExpression(lhs, p.contains(walkRhs(rhs)))
      case In(lhs, ListLiteral(list)) =>
        walkBinaryExpression(lhs, p.within(list.map(walkRhs): _*))
      case In(lhs, Parameter(name, _)) =>
        val coll = context.inlineParameter(name, classOf[util.Collection[_]])
        val args = coll.asScala.toSeq.asInstanceOf[Seq[Object]]
        walkBinaryExpression(lhs, p.within(args: _*))
      case expr: RightUnaryOperatorExpression =>
        walkRightUnaryOperatorExpression(expr)
      case expr: FunctionInvocation =>
        walkFunctionInvocation(expr)
      case HasLabels(Variable(name), labelNames) =>
        val labels = labelNames.map {
          case LabelName(labelName) => labelName
        }
        g.start().select(name).hasLabel(labels: _*)
      case Parameter(name, _: BooleanType) =>
        g.start().constant(context.parameter(name)).is(p.isEq(true))
      case l: BooleanLiteral =>
        g.start().constant(walkRhs(l)).is(p.isEq(true))
      case PatternExpression(RelationshipsPattern(relationshipChain)) =>
        val traversal = g.start()
        WhereWalker.walkRelationshipChain(context, traversal, relationshipChain)
        traversal
      case _ =>
        context.unsupported("expression", node)
    }
  }

  private def walkBinaryExpression(lhs: Expression, rhs: P): GremlinSteps[T, P] = {
    lhs match {
      case Variable(lvar) =>
        g.start().select(lvar).where(rhs)
      case Property(Variable(varName), PropertyKeyName(keyName)) =>
        g.start().select(freshIds.getOrElse(varName, varName)).values(keyName).is(rhs)
      case function: FunctionInvocation =>
        walkFunctionInvocation(function).is(rhs)
      case _ =>
        context.unsupported("binary expression lhs", lhs)
    }
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

  private def walkFunctionInvocation(function: FunctionInvocation): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    val FunctionInvocation(_, FunctionName(name), _, args) = function
    name.toLowerCase match {
      case "exists" =>
        args.head match {
          case Variable(varName) =>
            g.start().select(varName).is(p.neq(NULL))
          case Property(Variable(varName), PropertyKeyName(keyName)) =>
            g.start().select(varName).has(keyName)
        }
      case "length" =>
        args.head match {
          case Variable(varName) =>
            g.start().select(varName).count(Scope.local)
        }
      case "type" =>
        args.head match {
          case Variable(varName) =>
            g.start().select(varName).label()
        }
      case _ =>
        context.unsupported("expression", function)
    }
  }

  private def walkRhs(rhs: Expression): AnyRef = {
    expressionValue(rhs, context).asInstanceOf[AnyRef]
  }

  def walkRelationshipChain(relationshipChain: RelationshipChain) {
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
        RelationshipPatternWalker.walk(context, g, r)
      case n =>
        context.unsupported("pattern predicate", n)
    }
  }
}
