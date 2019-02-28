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

import org.apache.tinkerpop.gremlin.process.traversal.Pop
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.symbols.{ListType, RelationshipType}

object MatchWalker {

  def walkClause[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P], node: Match): Unit = {
    new MatchWalker(context, g).walkClause(node)
  }

  def walkPatternParts[T, P](
      context: WalkerContext[T, P],
      g: GremlinSteps[T, P],
      patternParts: Seq[PatternPart],
      whereOption: Option[Where]): Unit = {
    new MatchWalker(context, g).walkPatternParts(patternParts, whereOption)
  }
}

private class MatchWalker[T, P](context: WalkerContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Match): Unit = {
    val Match(optional, Pattern(patternParts), _, whereOption) = node
    if (optional) {
      walkOptionalMatch(patternParts, whereOption)
    } else {
      walkPatternParts(patternParts, whereOption)
    }
  }

  private def walkOptionalMatch(patternParts: Seq[PatternPart], whereOption: Option[Where]): Unit = {
    ensureFirstStatement(g, context)

    val nullG = g.start().constant(NULL)
    val contextNullG = context.copy()
    getPathTraversalAliases(patternParts.head)
      .map(name => contextNullG.alias(name).getOrElse(name))
      .foreach(nullG.as)

    val subG = g.start()
    MatchWalker.walkPatternParts(context, subG, patternParts, whereOption)

    g.choose(subG, subG, nullG)
  }

  def walkPatternParts(patternParts: Seq[PatternPart], whereOption: Option[Where]): Unit = {
    patternParts.foreach {
      case EveryPath(patternElement) =>
        PatternWalker.walk(context, g, patternElement)
      case NamedPatternPart(Variable(pathName), EveryPath(patternElement)) =>
        PatternWalker.walk(context, g, patternElement, Some(pathName))
        g.as(MATCH_END + pathName).path().from(Tokens.MATCH_START + pathName).as(pathName)
      case n =>
        context.unsupported("match pattern", n)
    }

    whereOption.foreach(WhereWalker.walk(context, g, _))

    reselectVarLengthPathRelationshipLists(patternParts)
  }

  private def reselectVarLengthPathRelationshipLists(patternParts: Seq[PatternPart]): Unit = {
    patternParts
      .flatMap(_.element.allVariables)
      .filter(
        qualifiedType(_, context) match {
          case (_: ListType) :: (_: RelationshipType) :: Nil => true
          case _                                             => false
        }
      )
      .foreach {
        case Variable(name) =>
          g.optional(g.start().select(Pop.all, name).as(name))
      }
  }
}
