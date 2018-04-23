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

import org.opencypher.gremlin.translation.Tokens.UNUSED
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * Remove select in case of single key and [[UNUSED]] workaround
  */
object ProjectionSimplifications extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    mapTraversals(replace({
      case As(stepLabel) :: SelectK(key1, key2) :: Group :: rest if eqUnused(stepLabel, key2) =>
        SelectK(key1) :: Group :: removeSelectFromBy(key1, rest)
      case As(stepLabel) :: SelectK(key1, key2) :: Project(keys @ _*) :: rest if eqUnused(stepLabel, key2) =>
        SelectK(key1) :: Project(keys: _*) :: removeSelectFromBy(key1, rest)
      case As(stepLabel) :: SelectK(key1, key2) :: Fold :: Project(keys @ _*) :: rest if eqUnused(stepLabel, key2) =>
        SelectK(key1) :: Fold :: Project(keys: _*) :: removeSelectFromBy(key1, rest)
    }))(steps)
  }

  private def removeSelect(key: String, steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    steps match {
      case Fold :: Project(keys @ _*) :: rest =>
        Fold :: Project(keys: _*) :: removeSelectFromBy(key, rest)
      case Unfold :: SelectK(key1) :: rest if key == key1 =>
        Unfold :: rest
      case SelectK(key1) :: rest if key == key1 && rest.nonEmpty =>
        rest
      case SelectK(key1) :: rest if key == key1 && rest.isEmpty =>
        Identity :: Nil
      case _ =>
        steps
    }
  }

  private def removeSelectFromBy(key: String, steps: List[GremlinStep]): List[GremlinStep] = {
    val (bys, rest) = steps.span(s => s.isInstanceOf[By])

    bys.map {
      case By(projection, None) =>
        By(removeSelect(key, projection), None)
      case step @ _ => step
    } ::: rest
  }

  private def eqUnused(stepLabel: String, key2: String) = {
    stepLabel == UNUSED && key2 == UNUSED
  }
}
