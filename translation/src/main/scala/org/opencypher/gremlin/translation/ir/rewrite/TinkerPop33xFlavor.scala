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
import org.opencypher.gremlin.translation.traversal.DeprecatedOrderAccessor

/**
  * todo
  */
object TinkerPop33xFlavor extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    Seq(
      rewiriteOrder(_)
    ).foldLeft(steps) { (steps, rewriter) =>
      mapTraversals(rewriter)(steps)
    }
  }

  private def rewiriteOrder(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case By(traversal, Some(org.apache.tinkerpop.gremlin.process.traversal.Order.asc)) :: rest =>
        By(traversal, Some(DeprecatedOrderAccessor.incr)) :: rest
      case By(traversal, Some(org.apache.tinkerpop.gremlin.process.traversal.Order.desc)) :: rest =>
        By(traversal, Some(DeprecatedOrderAccessor.decr)) :: rest

    })(steps)
  }
}
