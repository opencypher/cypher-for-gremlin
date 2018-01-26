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

import org.neo4j.cypher.internal.frontend.v3_2.InputPosition
import org.neo4j.cypher.internal.frontend.v3_2.ast._
import org.opencypher.gremlin.translation.Tokens.START
import org.opencypher.gremlin.translation.TranslationBuilder
import org.opencypher.gremlin.translation.walker.NodeUtils.getPathTraversalAliases

/**
  * AST walker that handles translation
  * of the `MERGE` clause nodes in the Cypher AST.
  */
object MergeWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: TranslationBuilder[T, P], node: Merge) {
    new MergeWalker(context, g).walkClause(node)
  }
}

private class MergeWalker[T, P](context: StatementContext[T, P], g: TranslationBuilder[T, P]) {

  def walkClause(node: Merge): TranslationBuilder[T, P] = {
    val Merge(Pattern(patternParts), actions: Seq[MergeAction]) = node
    walkMerge(g, patternParts, actions)
  }

  private def walkMerge(
      g: TranslationBuilder[T, P],
      patternParts: Seq[PatternPart],
      actions: Seq[MergeAction]): TranslationBuilder[T, P] = {

    if (context.isFirstStatement) {
      g.inject(START)
    }

    val whereExpressions = patternParts.flatMap(extractWhereExpressions)
    val fakePosition = InputPosition(0, 0, 0)
    val whereOption = whereExpressions match {
      case Seq()      => Option.empty
      case _ +: Seq() => Option(Where(whereExpressions.head)(fakePosition));
      case _          => Option(Where(Ands(Set(whereExpressions: _*))(fakePosition))(fakePosition))
    }

    // sub-graphs for coalesce
    val matchSubG = g.start()
    MatchWalker.walkPatternParts(context.copy(), matchSubG, patternParts, whereOption)
    val createSubG = g.start()
    CreateWalker.walkClause(context, createSubG, Create(Pattern(patternParts)(fakePosition))(fakePosition))

    actions.foreach {
      case OnMatch(action: SetClause)  => SetWalker.walkClause(context, matchSubG, action)
      case OnCreate(action: SetClause) => SetWalker.walkClause(context, createSubG, action)
    }

    val pathAliases = getPathTraversalAliases(patternParts.head.element)
    if (pathAliases.length > 1) {
      pathAliases.foreach(createSubG.as)
      pathAliases.foreach(matchSubG.as)
      g.coalesce(
        matchSubG.selectLabels(pathAliases: _*),
        createSubG.selectLabels(pathAliases: _*)
      )
    } else {
      g.coalesce(matchSubG, createSubG).as(pathAliases.head)
    }
  }

  private def extractWhereExpressions(node: ASTNode): Seq[Expression] = node match {
    case n: NodePattern                                         => extractWhereExpressionsFrom(n)
    case RelationshipPattern(Some(id), _, _, Some(props), _, _) => propertyPredicates(id, props)
    case EveryPath(pattern)                                     => extractWhereExpressions(pattern)
    case RelationshipChain(element: PatternElement, relationship: RelationshipPattern, rightNode: NodePattern) =>
      extractWhereExpressions(element) ++ extractWhereExpressions(relationship) ++ extractWhereExpressions(rightNode)
    case _ => Seq.empty[Expression]
  }

  private def extractWhereExpressionsFrom(node: NodePattern) = {
    val NodePattern(Some(id), labels, props) = node
    var expressions = Seq[Expression]()
    if (labels.nonEmpty) expressions ++= Vector(HasLabels(id.copyId.asInstanceOf[Expression], labels)(node.position))
    if (props.isDefined) expressions ++= propertyPredicates(id, props.get)
    expressions
  }

  private def propertyPredicates(id: Variable, props: Expression): Seq[Expression] = props match {
    case mapProps: MapExpression =>
      mapProps.items.map {
        // is taken from MatchPredicateNormalizer
        // (MATCH (a {a: 1, b: 2}) => MATCH (a) WHERE a.a = 1 AND a.b = 2)
        case (propId, expression) =>
          Equals(Property(id.copyId, propId)(mapProps.position), expression)(mapProps.position)
      }
    case expr: Expression =>
      Seq[Expression](Equals(id.copyId, expr)(expr.position))
    case _ =>
      Seq.empty[Expression]
  }
}
