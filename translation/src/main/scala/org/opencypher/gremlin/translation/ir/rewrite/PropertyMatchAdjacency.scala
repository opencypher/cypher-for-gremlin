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
package org.opencypher.gremlin.translation.ir.rewrite
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.translation.ir.rewrite.Rewriting._

import scala.collection.mutable

object PropertyMatchAdjacency extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val hasSteps = new mutable.HashMap[String, mutable.Set[GremlinStep]] with mutable.MultiMap[String, GremlinStep]

    val rewrittenAliases = extract(
      steps, {
        case As(stepLabel) :: _                   => stepLabel
        case Repeat(_ :: As(stepLabel) :: _) :: _ => stepLabel
      }
    ).groupBy(identity)
      .flatMap {
        case (alias, _ :: Nil) => Some(alias)
        case _                 => None // ignore shadowed aliases
      }
      .toSet

    if (rewrittenAliases.isEmpty) {
      return steps
    }

    extract(
      steps, {
        case WhereT(And(andTraversals @ _*) :: Nil) :: _ =>
          whereExtractor(andTraversals)
        case WhereT(whereTraversal) :: _ =>
          whereExtractor(whereTraversal :: Nil)
      }
    ).flatten.filter {
      case (stepLabel, _) => rewrittenAliases.contains(stepLabel)
    }.foreach {
      case (stepLabel, step) => hasSteps.addBinding(stepLabel, step)
    }

    val aliasesWhereFilter = whereFilter(rewrittenAliases) _
    val firstPass = replace(
      steps, {
        case As(stepLabel) :: rest if hasSteps.contains(stepLabel) =>
          val steps = hasSteps(stepLabel).toList
          As(stepLabel) +: (steps ++ rest)
        case Repeat(head :: As(stepLabel) :: repeatRest) :: rest if hasSteps.contains(stepLabel) =>
          val steps = hasSteps(stepLabel).toList
          Repeat(head :: As(stepLabel) +: (steps ++ repeatRest)) :: rest
        case WhereT(And(andTraversals @ _*) :: Nil) :: rest =>
          aliasesWhereFilter(andTraversals)
            .map(_ :: rest)
            .getOrElse(rest)
        case WhereT(whereTraversal) :: rest =>
          aliasesWhereFilter(whereTraversal :: Nil)
            .map(_ :: rest)
            .getOrElse(rest)
      }
    )

    firstPass
  }

  private def whereExtractor(traversals: Seq[Seq[GremlinStep]]): Seq[(String, GremlinStep)] = {
    traversals.flatMap {
      case SelectK(stepLabel) :: Values(propertyKey) :: Is(predicate) :: Nil =>
        Some((stepLabel, HasP(propertyKey, predicate)))
      case SelectK(stepLabel) :: (hasLabel: HasLabel) :: Nil =>
        Some((stepLabel, hasLabel))
      case _ =>
        None
    }
  }

  private def whereFilter(aliases: Set[String])(traversals: Seq[Seq[GremlinStep]]): Option[GremlinStep] = {
    val newTraversals = traversals.flatMap {
      case SelectK(alias) :: Values(_) :: Is(_) :: Nil if aliases.contains(alias) => None
      case SelectK(alias) :: (_: HasLabel) :: Nil if aliases.contains(alias)      => None
      case other                                                                  => Some(other)
    }.toList
    newTraversals match {
      case Nil              => None
      case traversal :: Nil => Some(WhereT(traversal))
      case _                => Some(WhereT(And(newTraversals: _*) :: Nil))
    }
  }
}
