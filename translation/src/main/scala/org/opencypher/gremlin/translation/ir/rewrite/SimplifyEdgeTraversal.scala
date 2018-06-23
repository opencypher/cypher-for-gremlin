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

/**
  * Patterns that only match a directed relation and begin the traversal
  * can be simplified to a single graph step.
  */
object SimplifyEdgeTraversal extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val replacement = steps match {
      case Vertex :: InE(labels @ _*) :: As(selectKey) :: rest  => replaceEdge(labels, selectKey, rest)
      case Vertex :: OutE(labels @ _*) :: As(selectKey) :: rest => replaceEdge(labels, selectKey, rest)
      case _                                                    => None
    }
    replacement.getOrElse(steps)
  }

  private def replaceEdge(
      labels: Seq[String],
      selectKey: String,
      afterAs: Seq[GremlinStep]): Option[Seq[GremlinStep]] = {
    val (hasSteps, afterHas) = splitHasSteps(afterAs)
    val afterEdge = matchAfterEdge(afterHas, selectKey)
    afterEdge match {
      case Some(rest) => Some(Seq(Edge, As(selectKey)) ++ labels.map(HasLabel(_)) ++ hasSteps ++ rest)
      case None       => None
    }
  }

  private def splitHasSteps(steps: Seq[GremlinStep]): (Seq[GremlinStep], Seq[GremlinStep]) = {
    val hasSteps = steps.takeWhile(_.isInstanceOf[HasP])
    val rest = steps.drop(hasSteps.size)
    (hasSteps, rest)
  }

  private def matchAfterEdge(steps: Seq[GremlinStep], selectKey: String): Option[Seq[GremlinStep]] = {
    steps match {
      case InV :: SelectK(`selectKey`) :: rest  => Some(rest)
      case OutV :: SelectK(`selectKey`) :: rest => Some(rest)
      case _                                    => None
    }
  }
}
