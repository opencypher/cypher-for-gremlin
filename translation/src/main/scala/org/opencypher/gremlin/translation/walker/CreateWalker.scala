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
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.SemanticDirection.INCOMING
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.ASTNode

import scala.collection.mutable

/**
  * AST walker that handles translation
  * of the `CREATE` clause nodes in the Cypher AST.
  */
object CreateWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Create): Unit = {
    new CreateWalker(context, g).walk(node.pattern.patternParts)
  }

}

private class CreateWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {
  private val nodeHistory = new mutable.Stack[String]

  def walk(patternParts: Seq[PatternPart]): Unit = {
    context.markFirstStatement()
    patternParts.foreach {
      case EveryPath(n: PatternElement) =>
        walkPattern(n)
      case n =>
        context.unsupported("create pattern", n)
    }
  }

  /**
    * For relationship like this: `(n1)-[r1]->(n2)<-[r2]-(n3)`
    * creation order would be: `n1 n2 r1 n3 r2`
    */
  private def walkPattern(patternElement: PatternElement): Unit = {
    flattenRelationshipChain(patternElement).foreach {
      case n: NodePattern =>
        walkNodePattern(n)
      case n: RelationshipPattern =>
        walkRelationshipPattern(n)
      case n =>
        context.unsupported("create pattern", n)
    }
  }

  private def walkNodePattern(nodePattern: NodePattern): Unit = {
    nodePattern match {
      case NodePattern(Some(Variable(name)), labels, propertiesOption) =>
        nodeHistory.push(name)

        context.alias(name) match {
          case Some(_) =>
            validateDeclaredNode(name, labels, propertiesOption)
            return
          case _ =>
        }

        if (labels.isEmpty) {
          g.addV().as(name)
        } else {
          g.addV(labels.head.name).as(name)
        }

        val properties = getPropertiesMap(propertiesOption)
        walkProperties(properties)
      case _ =>
        context.unsupported("node pattern", nodePattern)
    }
  }

  private def walkRelationshipPattern(relationshipPattern: RelationshipPattern): Unit = {
    relationshipPattern match {
      case RelationshipPattern(_, Nil, _, _, _, _) => // Ignored
      case RelationshipPattern(Some(Variable(rName)), types, _, propertiesOption, direction, _) =>
        val n2Name = nodeHistory.pop
        val n1Name = nodeHistory.pop
        nodeHistory.push(n2Name)

        val typeNames = types.map {
          case RelTypeName(relName) => relName
        }
        val properties = getPropertiesMap(propertiesOption)

        for (typeName <- typeNames) {
          g.addE(typeName)
          direction match {
            case INCOMING => g.from(n2Name).to(n1Name)
            case _        => g.from(n1Name).to(n2Name)
          }
          g.as(rName)

          walkProperties(properties)
        }
      case _ =>
        context.unsupported("relationship pattern", relationshipPattern)
    }
  }

  def walkProperties(properties: Seq[(String, Expression)]): Unit = {
    properties.filter {
      case (_, Null()) => false
      case _           => true
    }.foreach {
      case (key, expression) =>
        val traversal = ExpressionWalker.walkLocal(context, g, expression)
        g.property(key, traversal)
    }
  }

  private def flattenRelationshipChain(node: ASTNode): Vector[ASTNode] = {
    flattenRelationshipChain(Vector(), node)
  }

  private def flattenRelationshipChain(acc: Vector[ASTNode], node: ASTNode): Vector[ASTNode] = {
    node match {
      case RelationshipChain(left, relationship, right) =>
        acc ++
          flattenRelationshipChain(Vector(), left) ++
          flattenRelationshipChain(Vector(), right) ++
          Vector(relationship)
      case n =>
        acc :+ n
    }
  }

  private def getPropertiesMap(node: Option[Expression]): Seq[(String, Expression)] = {
    node.flatMap {
      case MapExpression(p) => Some(p)
      case _                => None
    }.getOrElse(Nil).flatMap {
      case (PropertyKeyName(key), expression) => Seq(key -> expression)
      case _                                  => Nil
    }
  }

  private def validateDeclaredNode(name: String, labels: Seq[LabelName], properties: Option[Expression]): Unit = {
    if (labels.nonEmpty || getPropertiesMap(properties).nonEmpty) {
      throw new SyntaxException(
        s"Can't create node '$name' with labels or properties here." +
          s" The variable is already declared in this context")
    }
  }
}
