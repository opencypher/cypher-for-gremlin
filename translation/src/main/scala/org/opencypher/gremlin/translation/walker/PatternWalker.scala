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

import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.expressions.SemanticDirection._
import org.opencypher.v9_0.expressions.{UnsignedDecimalIntegerLiteral => UDIL, _}
import org.opencypher.v9_0.util.ASTNode
import org.opencypher.v9_0.util.InputPosition.NONE

/**
  * AST walker that handles translation
  * of match pattern nodes of the Cypher AST.
  */
object PatternWalker {
  def walk[T, P](
      context: WalkerContext[T, P],
      g: GremlinSteps[T, P],
      node: PatternElement,
      pathName: Option[String] = None,
      startNewTraversal: Boolean = true): Unit = {
    new PatternWalker(context, g).walk(node, pathName, startNewTraversal)
  }
}

class PatternWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {
  private def __ = g.start()

  def walk(node: PatternElement, pathName: Option[String], startNewTraversal: Boolean): Unit = {
    val chain = flattenRelationshipChain(node)
    val (namedChain, aliases) = ensurePatternsHasNames(chain)

    if (startNewTraversal) {
      context.markFirstStatement()
      g.V()
    } else {
      namedChain.headOption match {
        case Some(NodePattern(Some(Variable(name)), _, _, _)) => g.select(name)
        case _                                                =>
      }
    }

    pathName.foreach(name => g.as(MATCH_START + name))

    namedChain.foreach {
      case node: NodePattern =>
        walkNode(node)
      case relationship: RelationshipPattern =>
        walkRelationship(pathName, relationship)
      case n =>
        context.unsupported("pattern element", n)
    }

    val undirected = namedChain.exists {
      case RelationshipPattern(_, _, _, _, BOTH, _, _) => true
      case _                                           => false
    }
  }

  private def walkNode(node: NodePattern): Unit = {
    val NodePattern(variableOption, labels, properties, _) = node
    val variable @ Variable(name) = variableOption
      .getOrElse(Variable(context.generateName())(NONE))
    asUniqueName(name, g, context)
    labels.foreach(label => g.hasLabel(label.name))
    properties.map(hasProperties(variable, _)).foreach(g.flatMap)
  }

  val traversalStepsHardLimit: Int = 10

  private def walkRelationship(pathName: Option[String], relationship: RelationshipPattern): Unit = {
    val RelationshipPattern(variableOption, types, length, properties, direction, _, _) = relationship
    val typeNames = types.map { case RelTypeName(relName) => relName }.distinct

    val directionT = g.start()
    direction match {
      case BOTH     => directionT.bothE(typeNames: _*).dedup().by(__.path())
      case INCOMING => directionT.inE(typeNames: _*)
      case OUTGOING => directionT.outE(typeNames: _*)
    }
    variableOption.foreach {
      case variable @ Variable(name) =>
        asUniqueName(name, directionT, context)
        properties.map(hasProperties(variable, _)).foreach(directionT.flatMap)
    }
    pathName.foreach(name => directionT.aggregate(PATH_EDGE + name))
    direction match {
      case BOTH     => directionT.otherV()
      case INCOMING => directionT.outV()
      case OUTGOING => directionT.inV()
    }

    val pathStart = PATH_START + pathName.getOrElse(context.generateName().trim())
    g.as(pathStart)

    val p = context.dsl.predicates()
    length match {
      case None =>
        // -[]->
        g.flatMap(directionT)
      case Some(None | Some(Range(None, None))) =>
        // -[*]->
        // -[*..]->
        g.repeat(directionT)
          .emit()
          .times(traversalStepsHardLimit)
      case Some(Some(range)) =>
        range match {
          case Range(Some(UDIL(lower)), None) =>
            // -[*m..]->
            g.emit(__.loops().is(p.gte(lower.toInt)))
              .repeat(directionT)
              .times(traversalStepsHardLimit)
          case Range(None, Some(UDIL("0"))) =>
            // -[*..0]->
            g.limit(0)
          case Range(None, Some(UDIL(upper))) =>
            // -[*..n]->
            g.repeat(directionT)
              .emit()
              .times(upper.toInt)
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) if lower == upper =>
            // -[*n]->
            g.times(lower.toInt)
              .repeat(directionT)
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) if lower.toInt > upper.toInt =>
            // -[*m..n]-> m>n
            g.limit(0)
          case Range(Some(UDIL(lower)), Some(UDIL(upper))) =>
            // -[*m..n]->
            g.emit(__.loops().is(p.gte(lower.toInt)))
              .repeat(directionT)
              .times(upper.toInt)
        }
      case _ =>
        context.unsupported("path pattern length", length)
    }

    (direction, length) match {
      case (BOTH, Some(_)) =>
        g.simplePath().from(pathStart)
      case _ =>
    }
  }

  private def ensurePatternsHasNames(chain: Vector[ASTNode]): (Vector[ASTNode], Vector[String]) = {
    val namedChain = chain.map {
      case n @ NodePattern(variable, labels, properties, baseNode) if variable.isEmpty =>
        val newVar = Variable(context.generateName())(n.position)
        NodePattern(Some(newVar), labels, properties, baseNode)(n.position)
      case r @ RelationshipPattern(variable, types, length, properties, direction, legacyTypeSeparator, baseRel)
          if variable.isEmpty =>
        val newVar = Variable(context.generateName())(r.position)
        RelationshipPattern(Some(newVar), types, length, properties, direction, legacyTypeSeparator, baseRel)(
          r.position)
      case t => t
    }

    val names = namedChain.flatMap {
      case NodePattern(Some(Variable(name)), _, _, _)                  => Some(name)
      case RelationshipPattern(Some(Variable(name)), _, _, _, _, _, _) => Some(name)
      case _                                                           => None
    }

    (namedChain, names)
  }

  private def hasProperties(variable: Variable, propertyMap: Expression): GremlinSteps[T, P] = {
    propertyMap match {
      case MapExpression(items) =>
        val traversal = g.start()
        items.foreach {
          case (name, expr) =>
            val equality = Equals(Property(variable, name)(NONE), expr)(NONE)
            WhereWalker.walk(context, traversal, equality)
        }
        traversal
      case _ =>
        context.unsupported("property map", propertyMap)
    }
  }
}
