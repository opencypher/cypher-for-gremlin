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

import org.opencypher.gremlin.translation.Tokens.{DELETE, DETACH_DELETE, NULL}
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * Removes surplus actions if `delete` or `detach delete` is not used in query
  */
object SimplifyDelete extends GremlinRewriter {
  def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val withChoose = foldTraversals(false)((acc, localSteps) => {
      acc || extract({
        case ChooseP(IsNode(), Aggregate(DELETE) :: Nil, Some(Aggregate(DETACH_DELETE) :: Nil)) :: _ => true
      })(localSteps).contains(true)
    })(steps)

    if (withChoose) {
      return steps
    }

    val aggregations = countInTraversals({
      case Aggregate(sideEffectKey) :: _ => sideEffectKey
    })(steps)

    val delete = aggregations.getOrElse(DELETE, 0) > 1
    val detachDelete = aggregations.getOrElse(DETACH_DELETE, 0) > 1

    mapTraversals(
      replace({
          case SideEffect(Limit(0) :: Aggregate(DELETE) :: Nil) :: SideEffect(
                Limit(0) :: Aggregate(DETACH_DELETE) :: Nil) :: rest =>
            rest
          case Cap(DELETE) :: Unfold :: Dedup() :: Is(Neq(NULL)) :: SideEffect(_) :: Drop :: rest if !delete =>
            rest
          case Cap(DETACH_DELETE) :: Unfold :: Dedup() :: Is(Neq(NULL)) :: Drop :: rest if !detachDelete =>
            rest
        }))(steps)
  }
}
