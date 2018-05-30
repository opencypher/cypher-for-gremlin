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

import org.opencypher.gremlin.translation.Tokens.{NULL, START}
import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions.SemanticDirection.BOTH
import org.opencypher.v9_0.expressions._

object MatchWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Match): Unit = {
    new MatchWalker(context, g).walkClause(node)
  }

  def walkPatternParts[T, P](
      context: StatementContext[T, P],
      g: GremlinSteps[T, P],
      patternParts: Seq[PatternPart],
      whereOption: Option[Where]): Unit = {
    new MatchWalker(context, g).walkPatternParts(patternParts, whereOption)
  }
}

private class MatchWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Match): Unit = {
    val Match(optional, Pattern(patternParts), _, whereOption) = node
    if (optional) {
      walkOptionalMatch(patternParts, whereOption)
    } else {
      walkPatternParts(patternParts, whereOption)
    }
  }

  private def walkOptionalMatch(patternParts: Seq[PatternPart], whereOption: Option[Where]): Unit = {
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
      val nullAliases = pathAliases.map(name => contextNullG.alias(name).getOrElse(name))
      nullAliases.foreach(nullG.as)
      g.coalesce(subG.select(pathAliases: _*), nullG.select(nullAliases: _*))
    } else {
      g.coalesce(subG, nullG)
      asUniqueName(pathAliases.head, g, context)
    }
  }

  def walkPatternParts(patternParts: Seq[PatternPart], whereOption: Option[Where]): Unit = {
    patternParts.foreach {
      case EveryPath(patternElement) =>
        foldPatternElement(None, patternElement)
      case NamedPatternPart(Variable(pathName), EveryPath(patternElement)) =>
        foldPatternElement(Some(pathName), patternElement)
        g.path().as(pathName)
      case n =>
        context.unsupported("match pattern", n)
    }

    whereOption.foreach(WhereWalker.walk(context, g, _))
  }

  private def foldPatternElement(maybeName: Option[String], patternElement: PatternElement): Unit = {
    if (!context.isFirstStatement) {
      context.midTraversal(g)
    } else {
      g.V()
      context.markFirstStatement()
    }

    val chain = flattenRelationshipChain(patternElement)
    chain.foreach {
      case NodePattern(Some(Variable(name)), _, _) =>
        asUniqueName(name, g, context)
      case r: RelationshipPattern =>
        RelationshipPatternWalker.walk(maybeName, context, g, r)
      case n =>
        context.unsupported("relationship pattern element", n)
    }

    val undirected = chain.exists {
      case RelationshipPattern(_, _, _, _, BOTH, _) => true
      case _                                        => false
    }
    if (undirected) {
      val aliases = getPathTraversalAliases(patternElement)
      g.dedup(aliases: _*)
    }
  }
}
