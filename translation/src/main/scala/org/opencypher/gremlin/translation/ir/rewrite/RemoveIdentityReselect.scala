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
package org.opencypher.gremlin.translation.ir.rewrite

import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * This rule removes `select` steps that immediately follow an `as` step with the same label,
  * or are separated from the `as` step by one or more `has` steps.
  * Since the expected value is already in the traverser, this is a useless operation.
  * This rewrite also enables some cases of [[RemoveUnusedAliases]] rewrites.
  */
object RemoveIdentityReselect extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    mapTraversals(replace({
      case As(stepLabel) :: rest =>
        As(stepLabel) +: removeReselect(rest, stepLabel)
    }))(steps)
  }

  private def removeReselect(steps: Seq[GremlinStep], stepLabel: String): Seq[GremlinStep] = {
    val (filters, suffix) = steps.span {
      case _: HasP | _: HasLabel => true
      case _                     => false
    }
    suffix match {
      case SelectK(selectKey) :: rest if stepLabel == selectKey =>
        filters ++ rest
      case _ =>
        steps
    }
  }
}
