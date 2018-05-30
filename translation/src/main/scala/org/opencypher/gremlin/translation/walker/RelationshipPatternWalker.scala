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

import org.apache.tinkerpop.gremlin.process.traversal.Scope
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens.PATH_EDGE
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.expressions.SemanticDirection._
import org.opencypher.v9_0.expressions.{UnsignedDecimalIntegerLiteral => UDIL, _}

/**
  * AST walker that handles translation
  * of relationship pattern nodes of the Cypher AST.
  */
object RelationshipPatternWalker {

  def walk[T, P](
      maybeName: Option[String],
      context: StatementContext[T, P],
      g: GremlinSteps[T, P],
      node: RelationshipPattern): Unit = {
    new RelationshipPatternWalker(context, g).walk(maybeName, node)
  }
}

class RelationshipPatternWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  val traversalStepsHardLimit: Int = context.lowerBound(10)

  def walk(maybeName: Option[String], node: RelationshipPattern): Unit = {
    val RelationshipPattern(variableOption, types, length, _, direction, _) = node
    val typeNames = types.map { case RelTypeName(relName) => relName }.distinct

    val directionT = g.start()
    direction match {
      case BOTH     => directionT.bothE(typeNames: _*)
      case INCOMING => directionT.inE(typeNames: _*)
      case OUTGOING => directionT.outE(typeNames: _*)
    }
    variableOption.foreach {
      case LogicalVariable(name) => asUniqueName(name, directionT, context)
    }
    maybeName.foreach(pathName => directionT.aggregate(PATH_EDGE + pathName))
    direction match {
      case BOTH     => directionT.otherV()
      case INCOMING => directionT.outV()
      case OUTGOING => directionT.inV()
    }

    def pathLengthT = g.start().path().count(Scope.local)

    val p = context.dsl.predicates()
    length match {
      case None =>
        // -[]->
        g.map(directionT)
      case Some(None | Some(Range(None, None))) =>
        // -[*]->
        // -[*..]->
        g.repeat(directionT)
          .emit()
          .until(pathLengthT.is(p.gte(traversalStepsHardLimit)))
      case Some(Some(range)) =>
        range match {
          case Range(Some(UDIL(lower)), None) =>
            // -[*m..]->
            val lowerBound = context.lowerBound(lower.toInt)
            g.emit()
              .repeat(directionT)
              .until(pathLengthT.is(p.gte(traversalStepsHardLimit)))
              .where(pathLengthT.is(p.gte(lowerBound)))
          case Range(None, Some(UDIL(upper))) =>
            // -[*..n]->
            val upperBound = context.upperBound(upper.toInt)
            g.repeat(directionT)
              .emit()
              .until(pathLengthT.is(p.gte(upperBound)))
              .where(pathLengthT.is(p.lte(upperBound)))
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) if lower == upper =>
            // -[*n]->
            g.times(lower.toInt)
              .repeat(directionT)
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) =>
            // -[*m..n]->
            val lowerBound = context.lowerBound(lower.toInt)
            val upperBound = context.upperBound(upper.toInt)
            g.emit()
              .repeat(directionT)
              .until(pathLengthT.is(p.gte(upperBound)))
              .where(pathLengthT.is(p.between(lowerBound, upperBound + 1)))
        }
      case _ =>
        context.unsupported("path pattern length", length)
    }
  }
}
