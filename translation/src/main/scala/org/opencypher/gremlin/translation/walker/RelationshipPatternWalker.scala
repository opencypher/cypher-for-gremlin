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
import org.neo4j.cypher.internal.frontend.v3_2.SemanticDirection.{BOTH, INCOMING, OUTGOING}
import org.neo4j.cypher.internal.frontend.v3_2.ast.{UnsignedDecimalIntegerLiteral => UDIL, _}
import org.opencypher.gremlin.translation.GremlinSteps

/**
  * AST walker that handles translation
  * of relationship pattern nodes of the Cypher AST.
  */
object RelationshipPatternWalker {

  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: RelationshipPattern) {
    new RelationshipPatternWalker(context, g).walk(node)
  }
}

class RelationshipPatternWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  val traversalStepsHardLimit: Int = context.lowerBound(10)

  type TraversalFunction = GremlinSteps[T, P] => GremlinSteps[T, P]

  def walk(node: RelationshipPattern) {
    val RelationshipPattern(variableOption, types, length, _, direction, _) = node
    val typeNames = types.map { case RelTypeName(relName) => relName }.distinct

    val addVariableName: TraversalFunction = (g) =>
      variableOption match {
        case Some(Variable(name)) => g.as(name)
        case None                 => g
    }
    val addDirection: TraversalFunction = (g) =>
      direction match {
        case BOTH     => addVariableName(g.bothE(typeNames: _*)).otherV()
        case INCOMING => addVariableName(g.inE(typeNames: _*)).outV()
        case OUTGOING => addVariableName(g.outE(typeNames: _*)).inV()
    }

    val p = context.dsl.predicateFactory()
    val varPathDirection = addDirection(g.start())
    val pathLength: TraversalFunction = _.start().path().count(Scope.local)
    length match {
      case None =>
        // -[]->
        addDirection(g)
      case Some(None | Some(Range(None, None))) =>
        // -[*]->
        // -[*..]->
        g.repeat(varPathDirection)
          .emit()
          .until(pathLength(g).is(p.gte(traversalStepsHardLimit)))
      case Some(Some(range)) =>
        range match {
          case Range(Some(UDIL(lower)), None) =>
            // -[*m..]->
            val lowerBound = context.lowerBound(lower.toInt)
            g.emit()
              .repeat(varPathDirection)
              .until(pathLength(g).is(p.gte(traversalStepsHardLimit)))
              .where(pathLength(g).is(p.gte(lowerBound)))
          case Range(None, Some(UDIL(upper))) =>
            // -[*..n]->
            val upperBound = context.upperBound(upper.toInt)
            g.repeat(varPathDirection)
              .emit()
              .until(pathLength(g).is(p.gte(upperBound)))
              .where(pathLength(g).is(p.lte(upperBound)))
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) if lower == upper =>
            // -[*n]->
            g.times(lower.toInt)
              .repeat(varPathDirection)
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) =>
            // -[*m..n]->
            val lowerBound = context.lowerBound(lower.toInt)
            val upperBound = context.upperBound(upper.toInt)
            g.emit()
              .repeat(varPathDirection)
              .until(pathLength(g).is(p.gte(upperBound)))
              .where(pathLength(g).is(p.between(lowerBound, upperBound + 1)))
        }
      case _ =>
        context.unsupported("path pattern length", length)
    }
  }
}
