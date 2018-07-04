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

import org.apache.tinkerpop.gremlin.structure.Column
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model.{GremlinStep, _}

/**
  * This is a set of rewrites to adapt the translation to AWS Neptune.
  */
object NeptuneFlavor extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    Seq(
      injectWorkaround(_),
      deleteWorkaround(_),
      limit0Workaround(_),
      multipleLabelsWorkaround(_),
      traversalRewriters(_)
    ).foldLeft(steps) { (steps, rewriter) =>
      rewriter(steps)
    }
  }

  private def traversalRewriters(topLevelRewrites: Seq[GremlinStep]) = {
    Seq(
      expandListProperties(_)
    ).foldLeft(topLevelRewrites) { (steps, rewriter) =>
      mapTraversals(rewriter)(steps)
    }
  }

  private def expandListProperties(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case PropertyTC(_, key, Project(_*) :: bySteps) :: rest => {
        expandSub(key, bySteps) ++ rest
      }
    })(steps)
  }

  private def expandSub(key: String, bySteps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case By(expr, None) :: rest         => PropertyT(key, expr) +: expandSub(key, rest)
      case SelectC(Column.values) :: rest => rest
    })(bySteps)
  }

  private def injectWorkaround(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    steps match {
      case Inject(s) :: rest =>
        Vertex :: Limit(0) :: Inject(s) :: rest
      case _ =>
        steps
    }
  }

  private def deleteWorkaround(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case SideEffect(drop) :: Barrier :: Limit(0) :: rest =>
        drop ++ rest
    })(steps)
  }

  private def limit0Workaround(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Barrier :: Limit(0) :: rest =>
        SelectK(NONEXISTENT) :: rest
    })(steps)
  }

  private def multipleLabelsWorkaround(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    steps match {
      case Vertex :: (_: HasLabel) :: (_: HasLabel) :: _ =>
        Vertex +: Is(Neq(NONEXISTENT)) +: steps.drop(1)
      case _ =>
        steps
    }
  }
}
