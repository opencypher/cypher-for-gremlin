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

import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * This rewriter removes different sequences of steps that are no-op or otherwise useless.
  * These sequences can sometimes appear in the generated traversal
  * when results of several walkers are combined together.
  */
object RemoveUselessSteps extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    mapTraversals({
      firstPass _
      secondPass
    })(steps)
  }

  private def firstPass(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      // Remove `fold` and `unfold` pairs, since the former is an inverse of the latter.
      case Fold :: Unfold :: rest =>
        rest
      case Unfold :: Fold :: rest =>
        rest

      // Remove unused projections
      case Project(projectKey) :: By(Identity :: Nil, None) :: SelectK(selectKey) :: rest if projectKey == selectKey =>
        rest
    })(steps)
  }

  private def secondPass(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      // Remove duplicate `as` steps
      case As(stepLabel1) :: As(stepLabel2) :: rest if stepLabel1 == stepLabel2 =>
        As(stepLabel1) :: rest
    })(steps)
  }
}
