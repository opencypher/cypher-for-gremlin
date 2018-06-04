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
import org.opencypher.gremlin.translation.walker.NodeUtils.{getPathTraversalAliases, _}
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition.NONE

/**
  * AST walker that handles translation
  * of the `MERGE` clause nodes in the Cypher AST.
  */
object MergeWalker {

  def walkClause[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: Merge): Unit = {
    new MergeWalker(context, g).walkClause(node)
  }
}

private class MergeWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walkClause(node: Merge): GremlinSteps[T, P] = {
    val Merge(Pattern(patternParts), actions: Seq[MergeAction], _) = node
    walkMerge(g, patternParts, actions)
  }

  private def walkMerge(
      g: GremlinSteps[T, P],
      patternParts: Seq[PatternPart],
      actions: Seq[MergeAction]): GremlinSteps[T, P] = {
    ensureFirstStatement(g, context)

    val matchSubG = g.start()
    MatchWalker.walkPatternParts(context.copy(), matchSubG, patternParts, None)
    val createSubG = g.start()
    CreateWalker.walkClause(context, createSubG, Create(Pattern(patternParts)(NONE))(NONE))

    actions.foreach {
      case OnMatch(action: SetClause)  => SetWalker.walkClause(context, matchSubG, action)
      case OnCreate(action: SetClause) => SetWalker.walkClause(context, createSubG, action)
    }

    val pathAliases = getPathTraversalAliases(patternParts.head)
    if (pathAliases.length > 1) {
      g.coalesce(
        matchSubG.select(pathAliases: _*),
        createSubG.select(pathAliases: _*)
      )
    } else {
      g.coalesce(matchSubG, createSubG)
        .as(pathAliases.head)
    }
  }
}
