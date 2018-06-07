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
package org.opencypher.gremlin.translation.ir.verify
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model.GremlinStep

/**
  * This post-condition verifies that no accidental empty traversals are generated.
  */
object NoEmptyTraversals extends GremlinPostCondition {
  override def apply(steps: Seq[GremlinStep]): Option[String] = {
    val foundEmpty = foldTraversals(false)({ (acc, steps) =>
      acc || steps.isEmpty
    })(steps)

    if (foundEmpty) {
      Some("Empty traversal found in the produced translation! This is likely a bug.")
    } else {
      None
    }
  }
}
