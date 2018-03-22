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
  override val instance: Seq[GremlinStep] => Seq[GremlinStep] = { steps =>
    val hasSteps = new mutable.HashMap[String, mutable.Set[GremlinStep]] with mutable.MultiMap[String, GremlinStep]
    find(
      steps, {
        case WhereT(And(andTraversals @ _*) :: Nil) :: _ =>
          andTraversals.flatMap {
            case SelectK(stepLabel) :: Values(propertyKey) :: Is(predicate) :: Nil =>
              Some((stepLabel, HasP(propertyKey, predicate)))
            case SelectK(stepLabel) :: (hasLabel: HasLabel) :: Nil =>
              Some((stepLabel, hasLabel))
            case _ =>
              None
          }
      }
    ).flatten.foreach {
      case (stepLabel, step) => hasSteps.addBinding(stepLabel, step)
    }

    val firstPass = replace(
      steps, {
        case As(stepLabel) :: rest if hasSteps.contains(stepLabel) =>
          val steps = hasSteps(stepLabel).toList
          As(stepLabel) +: (steps ++ rest)
        case WhereT(And(andTraversals @ _*) :: Nil) :: rest =>
          val newAndTraversals = andTraversals.flatMap {
            case SelectK(_) :: Values(_) :: Is(_) :: Nil => None
            case SelectK(_) :: (_: HasLabel) :: Nil      => None
            case other                                   => Some(other)
          }
          if (newAndTraversals.nonEmpty) {
            WhereT(And(newAndTraversals: _*) :: Nil) :: rest
          } else {
            rest
          }
      }
    )

    firstPass
  }
}
