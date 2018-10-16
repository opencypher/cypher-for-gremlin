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

import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.symbols.{AnyType, CypherType}
import org.opencypher.v9_0.util.{ASTNode, InputPosition}

import scala.collection.JavaConverters._

/**
  * AST walker that handles translation
  * of the `SET` clause nodes in the Cypher AST.
  */
object SetWalker {
  def walkClause[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: ASTNode): Unit = {
    new SetWalker(context, g).walkClause(node)
  }
}

private class SetWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: ASTNode): Unit = {
    node match {
      case SetClause(items) =>
        walkSetClause(items)
      case Remove(items) =>
        walkRemoveClause(items)
      case _ =>
        context.unsupported("match clause", node)
    }

  }

  private def walkSetClause(items: Seq[SetItem]): Unit = {
    val p = context.dsl.predicates()
    items.foreach {
      case SetPropertyItem(Property(v @ Variable(variable), PropertyKeyName(key)), expression: Expression) =>
        setProperty(typeOf(v), variable, key, expression)
      case SetIncludingPropertiesFromMapItem(v @ Variable(variable), expression) =>
        asMap(expression).foreach {
          case (key, value) => setProperty(typeOf(v), variable, key, value)
        }
      case SetExactPropertiesFromMapItem(to @ Variable(toName), from: Variable) if isElement(from, context) =>
        g.select(toName).sideEffect(g.start().is(p.neq(NULL)).properties().drop())
        g.flatMap(asList(Seq(to, from), context)).map(CustomFunction.cypherCopyProperties())
      case SetExactPropertiesFromMapItem(v @ Variable(variable), expression) =>
        g.select(variable).sideEffect(g.start().is(p.neq(NULL)).properties().drop())
        asMap(expression).foreach {
          case (key, value) => setProperty(typeOf(v), variable, key, value)
        }
      case n =>
        context.unsupported("set clause", n)
    }
  }

  def asMap(map: Expression): Map[String, Expression] = {
    map match {
      case MapExpression(pairs) =>
        pairs.map {
          case (PropertyKeyName(key), expression) =>
            (key, expression)
        }.toMap
      case p: Parameter =>
        val map = inlineExpressionValue(p, context, classOf[java.util.Map[String, _]])
        map.asScala.map {
          case (name, value) => (name, toLiteral(value))
        }.toMap
    }
  }

  private def walkRemoveClause(items: Seq[RemoveItem]): Unit = {
    items.foreach {
      case RemovePropertyItem(Property(v @ Variable(variable), PropertyKeyName(key))) =>
        setProperty(typeOf(v), variable, key, Null()(InputPosition.NONE))
      case n =>
        context.unsupported("set clause", n)
    }
  }

  private def setProperty(cypherType: CypherType, variable: String, key: String, value: Expression): Unit = {
    val traversal = ExpressionWalker.walkProperty(context, g.start(), cypherType, key, value)
    g.select(variable).flatMap(notNull(traversal, context))
  }

  private def typeOf(expr: Expression): CypherType = {
    context.expressionTypes.getOrElse(expr, AnyType.instance)
  }
}
