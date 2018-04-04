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

import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * This is a set of rewrites to adapt the translation to Cosmos DB.
  */
object CosmosDbFlavor extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    Seq(
      rewriteValues(_),
      rewriteLoops(_)
    ).foldLeft(steps) { (steps, rewriter) =>
      mapTraversals(rewriter)(steps)
    }
  }

  private def rewriteValues(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Values(propertyKeys @ _*) :: rest => Properties() :: HasKey(propertyKeys: _*) :: Value :: rest
    })(steps)
  }

  private def rewriteLoops(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Inject(START) :: Repeat(_) :: Times(end) ::
            Cap(_) :: Unfold ::
            Skip(start) :: Limit(_) :: As(stepLabel) :: rest =>
        val range = start until end
        Inject(range: _*) :: As(stepLabel) :: rest
    })(steps)
  }
}
