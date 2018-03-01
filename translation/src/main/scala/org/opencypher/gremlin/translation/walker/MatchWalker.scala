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

import org.neo4j.cypher.internal.frontend.v3_3.ast._
import org.opencypher.gremlin.translation.Tokens.{NULL, START}
import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils._

object MatchWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Match) {
    new MatchWalker(context, g).walkClause(node)
  }

  def walkPatternParts[T, P](
      context: StatementContext[T, P],
      g: GremlinSteps[T, P],
      patternParts: Seq[PatternPart],
      whereOption: Option[Where]) {
    new MatchWalker(context, g).walkPatternParts(patternParts, whereOption)
  }
}

private class MatchWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Match) {
    val Match(optional, Pattern(patternParts), _, whereOption) = node
    if (optional) {
      walkOptionalMatch(patternParts, whereOption)
    } else {
      walkPatternParts(patternParts, whereOption)
    }
  }

  private def walkOptionalMatch(patternParts: Seq[PatternPart], whereOption: Option[Where]) {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    val subG = g.start()
    val contextSubG = context.copy()
    MatchWalker.walkPatternParts(contextSubG, subG, patternParts, whereOption)

    val nullG = g.start().constant(NULL)
    val contextNullG = context.copy()

    val pathAliases = patternParts.head match {
      case EveryPath(patternElement) =>
        getPathTraversalAliases(patternElement)
      case NamedPatternPart(Variable(pathName), EveryPath(patternElement)) =>
        getPathTraversalAliases(patternElement) :+ pathName
      case n =>
        context.unsupported("match pattern", n)
    }

    if (pathAliases.length > 1) {
      val nullAliases = pathAliases.map(ensureUniqueName(_, contextNullG))
      nullAliases.foreach(nullG.as)
      g.coalesce(subG.select(pathAliases: _*), nullG.select(nullAliases: _*))
    } else {
      g.coalesce(subG, nullG)
      asUniqueName(pathAliases.head, g, context)
    }
  }

  def walkPatternParts(patternParts: Seq[PatternPart], whereOption: Option[Where]) {
    patternParts.foreach {
      case EveryPath(patternElement) =>
        foldPatternElement(patternElement)
      case NamedPatternPart(Variable(pathName), EveryPath(patternElement)) =>
        foldPatternElement(patternElement)
        g.path().as(pathName)
      case n =>
        context.unsupported("match pattern", n)
    }

    whereOption.foreach(WhereWalker.walk(context, g, _))
  }

  private def foldPatternElement(patternElement: PatternElement) {
    if (!context.isFirstStatement) {
      context.midTraversal(g)
    } else {
      g.V()
      context.markFirstStatement()
    }
    flattenRelationshipChain(patternElement).foreach {
      case NodePattern(Some(Variable(name)), _, _) =>
        asUniqueName(name, g, context)
      case r: RelationshipPattern =>
        RelationshipPatternWalker.walk(context, g, r)
      case n =>
        context.unsupported("relationship pattern element", n)
    }
  }
}
