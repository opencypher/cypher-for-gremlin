/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
import org.apache.tinkerpop.gremlin.structure.Column
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.symbols._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `WHERE` clause nodes in the Cypher AST.
  */
object WhereWalker {
  def walk[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Where): Unit = {
    new WhereWalker(context, g).walk(node)
  }

  def walk[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], expression: Expression): Unit = {
    new WhereWalker(context, g).walk(expression)
  }
}

private class WhereWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

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
    g.where(walkBooleanExpression(expression))
  }

  private def walkBooleanExpression(node: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    node match {
      case _: BooleanLiteral | _: Parameter | _: FunctionInvocation =>
        walkExpression(node).is(p.isEq(true))
      case _ =>
        walkExpression(node)
    }
  }

  private def __ = g.start()

  private def walkExpression(node: Expression): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    node match {
      case AllIterablePredicate(FilterScope(Variable(freshId), Some(expr)), Variable(varName)) =>
        freshIds(freshId) = varName
        walkExpression(expr)

      case Variable(varName) =>
        __.select(freshIds.getOrElse(varName, varName))

      case Property(expr, PropertyKeyName(keyName: String)) =>
        val typ = context.expressionTypes.getOrElse(expr, AnyType.instance)
        val maybeExtractStep: Option[String => GremlinSteps[T, P]] = typ match {
          case NodeType.instance         => Some(__.values(_))
          case RelationshipType.instance => Some(__.values(_))
          case MapType.instance          => Some(__.select(_))
          case _                         => None
        }
        maybeExtractStep.map { extractStep =>
          walkExpression(expr).flatMap(extractStep(keyName))
        }.getOrElse {
          val key = StringLiteral(keyName)(InputPosition.NONE)
          asList(expr, key).map(CustomFunction.cypherContainerIndex()).is(p.neq(NULL))
        }

      case ContainerIndex(expr, idx) =>
        val typ = context.expressionTypes.getOrElse(expr, AnyType.instance)
        (typ, idx) match {
          case (_: ListType, l: IntegerLiteral) if l.value >= 0 =>
            walkExpression(expr).range(Scope.local, l.value, l.value + 1)
          case _ =>
            asList(expr, idx).map(CustomFunction.cypherContainerIndex()).is(p.neq(NULL))
        }

      case HasLabels(expr, labels) =>
        val traversal = walkExpression(expr)
        labels.foreach(label => traversal.hasLabel(label.name))
        traversal

      case IsNull(expr) =>
        expr match {
          case Property(propertyExpr, PropertyKeyName(keyName: String)) =>
            walkExpression(propertyExpr).hasNot(keyName)
          case _ =>
            walkExpression(expr).is(p.isEq(NULL))
        }
      case IsNotNull(expr) =>
        expr match {
          case Property(propertyExpr, PropertyKeyName(keyName: String)) =>
            walkExpression(propertyExpr).has(keyName)
          case _ =>
            walkExpression(expr).is(p.neq(NULL))
        }

      case Equals(lhs, rhs)             => walkPredicate(lhs, rhs, p.isEq)
      case Not(Equals(lhs, rhs))        => walkPredicate(lhs, rhs, p.neq)
      case LessThan(lhs, rhs)           => walkPredicate(lhs, rhs, p.lt)
      case LessThanOrEqual(lhs, rhs)    => walkPredicate(lhs, rhs, p.lte)
      case GreaterThan(lhs, rhs)        => walkPredicate(lhs, rhs, p.gt)
      case GreaterThanOrEqual(lhs, rhs) => walkPredicate(lhs, rhs, p.gte)
      case StartsWith(lhs, rhs)         => walkPredicate(lhs, rhs, p.startsWith)
      case EndsWith(lhs, rhs)           => walkPredicate(lhs, rhs, p.endsWith)
      case Contains(lhs, rhs)           => walkPredicate(lhs, rhs, p.contains)
      case In(lhs, rhs)                 => walkVargPredicate(lhs, rhs, a => p.within(a: _*))
      case Not(In(lhs, rhs))            => walkVargPredicate(lhs, rhs, a => p.without(a: _*))

      case Ands(ands) => __.and(ands.map(walkBooleanExpression).toSeq: _*)
      case Ors(ors)   => __.or(ors.map(walkBooleanExpression).toSeq: _*)
      case Not(rhs)   => __.not(walkBooleanExpression(rhs))

      case PatternExpression(RelationshipsPattern(relationshipChain)) =>
        val traversal = g.start()
        PatternWalker.walk(context, traversal, relationshipChain)
        traversal

      case l: Literal =>
        __.constant(l.value)

      case _ =>
        ExpressionWalker.walkLocal(context, g, node).is(p.neq(NULL))
    }
  }

  private def walkVargPredicate(lhs: Expression, rhs: Expression, predicate: Seq[AnyRef] => P): GremlinSteps[T, P] = {
    val value = traversalValueOption(rhs, context, context.parameter).orNull
    (value, rhs) match {
      case (list: java.lang.Iterable[_], _: ListLiteral) =>
        val lhsT = walkExpression(lhs)
        val seq = list.asScala.map(_.asInstanceOf[AnyRef]).toSeq
        lhsT.is(predicate(seq))
      case _ =>
        walkPredicate(lhs, rhs, a => predicate(Seq(a)))
    }
  }

  private def walkPredicate(lhs: Expression, rhs: Expression, predicate: AnyRef => P): GremlinSteps[T, P] = {
    val lhsT = walkExpression(lhs)

    rhs match {
      case Variable(varName) =>
        lhsT.where(predicate(freshIds.getOrElse(varName, varName)))
      case _: Literal | _: Null =>
        val rhsV = expressionValue(rhs, context)
        lhsT.is(predicate(rhsV))
      case _ =>
        val rhsName = context.generateName()
        val rhsT = walkExpression(rhs)
        rhsT
          .as(rhsName)
          .flatMap(lhsT)
          .where(predicate(rhsName))
    }
  }

  private def asList(expressions: Expression*): GremlinSteps[T, P] = {
    val keys = expressions.map(_ => context.generateName())
    val traversal = __.project(keys: _*)
    expressions.map(walkExpression).foreach(traversal.by)
    traversal.select(Column.values)
  }
}
