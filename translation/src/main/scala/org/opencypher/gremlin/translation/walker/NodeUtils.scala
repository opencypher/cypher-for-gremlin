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

import org.apache.tinkerpop.gremlin.structure.util.detached.{DetachedProperty, DetachedVertexProperty}
import org.neo4j.cypher.internal.frontend.v3_2.ast._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}

import scala.collection.JavaConverters._

object NodeUtils {
  def expressionValue[T, P](node: Expression, context: StatementContext[T, P]): Any = {
    traversalValueToJava(node, context, inline = false)
  }

  def inlineExpressionValue[T, P, R](node: Expression, context: StatementContext[T, P]): R = {
    traversalValueToJava(node, context, inline = true).asInstanceOf[R]
  }

  def traversalValueToJava[T, P](value: Any, context: StatementContext[T, P], inline: Boolean): Any = {
    value match {
      case Variable(varName) =>
        varName
      case Parameter(name, _) =>
        context.parameter(name, inline)
      case Null() =>
        Tokens.NULL
      case ListComprehension(_, Parameter(name, _)) =>
        context.parameter(name, inline)
      case l: Literal =>
        l.value
      case ListLiteral(expressions) =>
        traversalValueToJava(expressions, context, inline)
      case MapExpression(items) =>
        asDetachedVertex(items, context)
      case FunctionInvocation(_, _, _, Seq(args)) =>
        expressionValue(args, context)
      case seq: Seq[_] =>
        val mappedSeq = seq.map(traversalValueToJava(_, context, inline))
        new util.ArrayList(mappedSeq.asJava)
      case map: Map[_, _] =>
        val mappedMap = map.mapValues(traversalValueToJava(_, context, inline))
        new util.LinkedHashMap[Any, Any](mappedMap.asJava)
      case n =>
        n
    }
  }

  private def asDetachedVertex[T, P](
      items: Seq[(PropertyKeyName, Expression)],
      context: StatementContext[T, P]): Any = {
    val builder = DetachedVertexProperty.build().setId(0)

    items.foreach(item => {
      val (PropertyKeyName(name), expression) = item
      val value = expressionValue(expression, context)
      builder.addProperty(new DetachedProperty[Any](name, value))
    })

    builder.create()
  }

  def getPathTraversalAliases(patternElement: PatternElement): Vector[String] = {
    flattenRelationshipChain(patternElement).foldLeft(Vector.empty[String]) { (acc, element) =>
      element match {
        case NodePattern(Some(Variable(name)), _, _) =>
          acc :+ name
        case RelationshipPattern(Some(Variable(name)), _, _, _, _, _) =>
          acc :+ name
        case _ =>
          acc
      }
    }
  }

  def flattenRelationshipChain(node: ASTNode): Vector[ASTNode] = {
    flattenRelationshipChain(Vector(), node)
  }

  private def flattenRelationshipChain(acc: Vector[ASTNode], node: ASTNode): Vector[ASTNode] = {
    node match {
      case RelationshipChain(left, relationship, right) =>
        acc ++
          flattenRelationshipChain(Vector(), left) ++
          Vector(relationship) ++
          flattenRelationshipChain(Vector(), right)
      case n =>
        acc :+ n
    }
  }

  def asUniqueName[T, P](name: String, g: GremlinSteps[T, P], context: StatementContext[T, P]): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    if (context.referencedAliases.contains(name)) {
      val generated = context.generateName()
      g.as(generated).where(g.start().select(generated).where(p.isEq(name)))
    } else {
      context.referencedAliases.add(name)
      g.as(name)
    }
  }

  def ensureUniqueName[T, P](name: String, context: StatementContext[T, P]): String = {
    if (context.referencedAliases.contains(name)) {
      context.generateName()
    } else {
      name
    }
  }

  def setProperty[T, P](g: GremlinSteps[T, P], key: String, value: Any) {
    value match {
      case builder: GremlinSteps[T @unchecked, P @unchecked] =>
        g.property(key, builder)
      case null =>
        drop(g, key)
      case s: String if s.equals(Tokens.NULL) =>
        drop(g, key)
      case v: Vector[_] if v.isEmpty =>
        drop(g, key)
      case c: java.util.Collection[_] if c.isEmpty =>
        drop(g, key)
      case vector: Vector[_] =>
        val collection = new util.ArrayList[Any](vector.asJava)
        g.property(key, collection)
      case _ =>
        g.property(key, value)
    }
  }

  private def drop[T, P](g: GremlinSteps[T, P], key: String) = {
    g.sideEffect(g.start().properties(key).drop())
  }
}
