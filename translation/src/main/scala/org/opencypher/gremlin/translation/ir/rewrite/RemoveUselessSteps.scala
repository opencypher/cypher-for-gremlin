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
    mapTraversals(replace({
      // This rule removes `select` steps that immediately follow an `as` step with the same label.
      // Since the expected value is already in the traverser, this is a useless operation.
      // This rewrite also enables some cases of RemoveUnusedAliases rewrites.
      case As(stepLabel) :: SelectK(selectKey) :: rest if stepLabel == selectKey =>
        As(stepLabel) :: rest

      // This rule removes `fold` and `unfold` pairs, since the latter is an inverse of the former.
      case Fold :: Unfold :: rest =>
        rest
    }))(steps)
  }
}
