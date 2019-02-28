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
package org.opencypher.gremlin.translation.ir.verify

import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * This post-condition verifies that multiple labels are not used.
  * Such translation will not work in environments without multiple label support.
  */
object NoMultipleLabels extends GremlinPostCondition {
  override def apply(steps: Seq[GremlinStep]): Option[String] = {
    val multiLabels = foldTraversals(Seq.empty[String])({ (acc, steps) =>
      acc ++ extractVertexLabels(steps)
    })(steps).sorted.distinct
      .filter(_.contains("::"))

    if (multiLabels.nonEmpty) {
      Some(s"Multiple labels are not supported: ${multiLabels.mkString(", ")}")
    } else {
      None
    }
  }

  private def extractVertexLabels(steps: Seq[GremlinStep]): Seq[String] = {
    val added = extract({
      case AddV(label) :: _ => label
    })(steps)

    def extractFiltered(acc: Seq[String], steps: Seq[GremlinStep]): Seq[String] = {
      steps.headOption match {
        case Some(_: HasLabel) =>
          val (filters, rest) = steps.span(_.isInstanceOf[HasLabel])
          val multiLabel = filters.map({ case HasLabel(label) => label }).mkString("::")
          extractFiltered(acc :+ multiLabel, rest)
        case Some(_) =>
          extractFiltered(acc, steps.tail)
        case None =>
          acc
      }
    }

    val filtered = extractFiltered(Nil, steps)

    added ++ filtered
  }
}
