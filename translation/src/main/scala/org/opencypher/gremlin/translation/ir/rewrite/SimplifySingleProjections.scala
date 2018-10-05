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
  * This rule removes workarounds from effectively-single projections
  * and lifts projection traversals out of single aggregating projections.
  */
object SimplifySingleProjections extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    Seq(
      removeUnused(_),
      liftFoldProjection(_),
      mapSingleProjection(_)
    ).foldLeft(steps) { (steps, rewriter) =>
      mapTraversals(rewriter)(steps)
    }
  }

  private def removeUnused(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case As(stepLabel) :: SelectK(key1, key2) :: Group :: rest if eqUnused(stepLabel, key2) =>
        SelectK(key1) :: Group :: removeSelectFromBy(key1, rest)
      case As(stepLabel) :: SelectK(key1, key2) :: Project(keys @ _*) :: rest if eqUnused(stepLabel, key2) =>
        SelectK(key1) :: Project(keys: _*) :: removeSelectFromBy(key1, rest)
      case As(stepLabel) :: SelectK(key1, key2) :: Fold :: Project(keys @ _*) :: rest if eqUnused(stepLabel, key2) =>
        SelectK(key1) :: Fold :: Project(keys: _*) :: removeSelectFromBy(key1, rest)
    })(steps)
  }

  private def removeSelect(key: String, steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    steps match {
      case Project(keys @ _*) :: rest =>
        Project(keys: _*) :: removeSelectFromBy(key, rest)
      case Unfold :: Project(keys @ _*) :: rest =>
        Unfold :: Project(keys: _*) :: removeSelectFromBy(key, rest)
      case Fold :: Project(keys @ _*) :: rest =>
        Fold :: Project(keys: _*) :: removeSelectFromBy(key, rest)
      case Unfold :: SelectK(key1) :: rest if key == key1 =>
        Unfold :: rest
      case SelectK(key1) :: rest if key == key1 && rest.nonEmpty =>
        rest
      case SelectK(key1) :: rest if key == key1 && rest.isEmpty =>
        Identity :: Nil
      case _ =>
        removeSelectFromSubtraversals(key, steps)
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

  private def removeSelectFromSubtraversals(key: String, steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val (replaceable, rest) =
      steps.span(s => !s.isInstanceOf[Constant] || s.isInstanceOf[SelectC] || s.isInstanceOf[SelectK])

    replaceable.map({ step =>
      step.mapTraversals(subSteps => mapTraversals(removeSelect(key, _))(subSteps))
    }) ++ rest
  }

  private def eqUnused(stepLabel: String, key2: String): Boolean = {
    stepLabel == UNUSED && key2 == UNUSED
  }

  private def liftFoldProjection(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Project(key) :: By(traversal @ Unfold :: _, order) :: rest =>
        traversal ++ (Project(key) :: By(Identity :: Nil, order) :: rest)
    })(steps)
  }

  def mapSingleProjection(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Project(projectKey) :: By(traversal, None) :: SelectK(selectKey) :: rest if projectKey == selectKey =>
        traversal match {
          case Identity :: Nil => rest
          case _               => MapT(traversal) :: rest
        }
    })(steps)
  }
}
