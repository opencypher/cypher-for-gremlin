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

import org.apache.tinkerpop.gremlin.structure.Column
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.single
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.CypherExceptions
import org.opencypher.gremlin.translation.{GremlinSteps, Tokens}
import org.opencypher.gremlin.traversal.CustomFunction
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.symbols.{CypherType, NodeType, RelationshipType}
import org.opencypher.v9_0.util.{ASTNode, InputPosition}

import scala.collection.JavaConverters._

object NodeUtils {
  def expressionValue[T, P](node: Expression, context: WalkerContext[T, P]): AnyRef = {
    traversalValueToJava(node, context, context.parameter)
  }

  def inlineExpressionValue[T, P](node: Expression, context: WalkerContext[T, P]): AnyRef = {
    inlineExpressionValue(node, context, classOf[AnyRef])
  }

  def inlineExpressionValue[T, P, R <: AnyRef](node: Expression, context: WalkerContext[T, P], klass: Class[R]): R = {
    val parameterHandler = (name: String) => context.inlineParameter(name, klass)
    traversalValueToJava(node, context, parameterHandler).asInstanceOf[R]
  }

  def traversalValueToJava[T, P](
      value: Any,
      context: WalkerContext[T, P],
      parameterHandler: String => AnyRef): AnyRef = {
    value match {
      case Variable(varName) =>
        varName
      case Parameter(name, _) =>
        parameterHandler(name)
      case Null() =>
        Tokens.NULL
      case ListComprehension(_, Parameter(name, _)) =>
        parameterHandler(name)
      case l: Literal =>
        l.value
      case ListLiteral(expressions) =>
        traversalValueToJava(expressions, context, parameterHandler)
      case MapExpression(items) =>
        traversalValueToJava(items.toMap, context, parameterHandler)
      case FunctionInvocation(_, _, _, Seq(args)) =>
        expressionValue(args, context)
      case seq: Seq[_] =>
        val mappedSeq = seq.map(traversalValueToJava(_, context, parameterHandler))
        new util.ArrayList(mappedSeq.asJava)
      case map: Map[_, _] =>
        val mappedMap = map.mapValues(traversalValueToJava(_, context, parameterHandler))
        new util.LinkedHashMap[Any, Any](mappedMap.asJava)
      case _ =>
        context.unsupported("value expression", value)
    }
  }

  def getPathTraversalAliases(patternPart: PatternPart): Vector[String] = {
    patternPart match {
      case NamedPatternPart(Variable(pathName), EveryPath(patternElement)) =>
        getPathTraversalAliases(patternElement) :+ pathName
      case _ =>
        getPathTraversalAliases(patternPart.element)
    }
  }

  def getPathTraversalAliases(patternElement: PatternElement): Vector[String] = {
    flattenRelationshipChain(patternElement).foldLeft(Vector.empty[String]) { (acc, element) =>
      element match {
        case NodePattern(Some(Variable(name)), _, _, _) =>
          acc :+ name
        case RelationshipPattern(Some(Variable(name)), _, _, _, _, _, _) =>
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

  def asUniqueName[T, P](name: String, g: GremlinSteps[T, P], context: WalkerContext[T, P]): GremlinSteps[T, P] = {
    val p = context.dsl.predicates()
    context.alias(name) match {
      case Some(generated) =>
        g.as(generated).where(g.start().select(generated).where(p.isEq(name)))
      case _ =>
        g.as(name)
    }
  }

  def notNull[T, P](traversal: GremlinSteps[T, P], context: WalkerContext[T, P]): GremlinSteps[T, P] = {
    val g = context.dsl.steps()
    val p = context.dsl.predicates()
    g.start().choose(p.neq(NULL), traversal)
  }

  def emptyToNull[T, P](traversal: GremlinSteps[T, P], context: WalkerContext[T, P]): GremlinSteps[T, P] = {
    val g = context.dsl.steps()
    g.start().choose(traversal, traversal, g.start().constant(NULL))
  }

  def asList[T, P](expressions: Seq[Expression], context: WalkerContext[T, P]): GremlinSteps[T, P] = {
    val g = context.dsl.steps()
    if (expressions.isEmpty) {
      return g.start().constant(new util.ArrayList())
    }
    val keys = expressions.map(_ => context.generateName())
    val traversal = g.start().project(keys: _*)
    expressions.map(ExpressionWalker.walkLocal(context, g, _)).foreach(traversal.by)
    traversal.select(Column.values)
  }

  def ensureFirstStatement[T, P](traversal: GremlinSteps[T, P], context: WalkerContext[T, P]): Unit = {
    if (context.isFirstStatement) {
      traversal.inject(Tokens.START)
      context.markFirstStatement()
    }
  }

  def selectNestedAliases[T, P](keys: Seq[String], context: WalkerContext[T, P]): GremlinSteps[T, P] = {
    val g = context.dsl.steps().start()
    val mapName = context.generateName()
    g.as(mapName)
    keys
      .filter(context.alias(_).isEmpty)
      .map(alias => g.select(mapName).select(alias).as(alias))
    g
  }

  def reselectProjection[T, P](items: Seq[LogicalVariable], context: WalkerContext[T, P]): GremlinSteps[T, P] = {
    val traversal = context.dsl.steps().start()
    val name = context.generateName()
    if (items.lengthCompare(1) > 0) {
      traversal.as(name)
    }

    items.toStream.zipWithIndex.foreach {
      case (LogicalVariable(alias), i) =>
        if (i > 0) traversal.select(name)
        traversal.select(alias).as(alias)
        context.alias(alias)
      case _ =>
    }

    traversal
  }

  def setProperty[T, P](
      traversal: GremlinSteps[T, P],
      cypherType: CypherType,
      key: String,
      value: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    cypherType match {
      case _: NodeType         => traversal.property(single, key, value)
      case _: RelationshipType => traversal.property(key, value)
      case _                   => throw new UnsupportedOperationException(s"Unsupported property type: $cypherType")
    }
  }

  def toLiteral(obj: Any): Literal = {
    obj match {
      case _: java.lang.Integer | _: java.lang.Long =>
        SignedDecimalIntegerLiteral(String.valueOf(obj))(InputPosition.NONE)
      case d: java.lang.Double => DecimalDoubleLiteral(String.valueOf(d))(InputPosition.NONE)
      case s: java.lang.String => StringLiteral(s)(InputPosition.NONE)
      case true                => True()(InputPosition.NONE)
      case false               => False()(InputPosition.NONE)
      case null                => Null()(InputPosition.NONE)
    }
  }

  def runtimeValidation[T, P](
      throwIfTrue: GremlinSteps[T, P],
      exception: CypherExceptions,
      context: WalkerContext[T, P]): GremlinSteps[T, P] =
    context.dsl
      .steps()
      .start()
      .sideEffect(
        throwIfTrue
          .constant(exception.toString)
          .map(CustomFunction.cypherException())
      )

}
