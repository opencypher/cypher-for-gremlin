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
  * This rule removes intermediate projection in case it does not have any logic, and followed by final projection
  */
object RemoveIntermediateProjection extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val traversals = split(BeforeStep, {
      case Project(_*) => true
      case _           => false
    })(steps)

    val size = traversals.size
    if (size < 3) {
      return steps
    }

    val intermediateProjection = traversals(size - 2)
    val finalizationProjection = traversals.last

    if (finalizationProjection.size == intermediateProjection.size
        && finalizationProjection.head == intermediateProjection.head
        && onlyContainsIdentityOrSelect(intermediateProjection)) {

      val replacements = getReplacements(intermediateProjection)
      val singleProjection = replaceInBy(finalizationProjection, replacements)

      (traversals.take(size - 2) :+ singleProjection).flatten
    } else {
      steps
    }
  }

  def onlyContainsIdentityOrSelect(steps: Seq[GremlinStep]): Boolean = !steps.exists {
    case Project(_*)                 => false
    case By(Identity :: Nil, None)   => false
    case By(SelectK(_) :: Nil, None) => false
    case _                           => true
  }

  def getReplacements(intermediateProjection: Seq[GremlinStep]): Map[GremlinStep, GremlinStep] = {
    val Project(keys @ _*) = intermediateProjection.head
    val valueSteps = intermediateProjection.tail.map {
      case By(Identity :: Nil, None)     => Identity
      case By(SelectK(key) :: Nil, None) => SelectK(key)
    }

    val keySteps = keys.map(key => SelectK(key))

    (keySteps zip valueSteps).toMap
  }

  def replaceInBy(projection: Seq[GremlinStep], replace: Map[GremlinStep, GremlinStep]): Seq[GremlinStep] =
    projection.map {
      case By(steps, order) => By(replace(steps.head) +: steps.tail, order)
      case s                => s
    }

}
