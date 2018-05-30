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
import org.opencypher.gremlin.translation.ir.model._

/**
  * This post-condition verifies that custom functions are not used.
  * Such translation will not work in environments without the Cypher plugin.
  */
object NoCustomFunctions extends GremlinPostCondition {
  override def apply(steps: Seq[GremlinStep]): Option[String] = {
    val all = foldTraversals(Seq.empty[String])({ (acc, steps) =>
      acc ++ extractFunctionNames(steps)
    })(steps).sorted.distinct

    if (all.nonEmpty) {
      Some(s"Custom functions and predicates are not supported: ${all.mkString(", ")}")
    } else {
      None
    }
  }

  private def extractFunctionNames(steps: Seq[GremlinStep]): Seq[String] = {
    val functions = extract({
      case MapF(function) :: _ => function.getName
    })(steps)

    val predicates = extract({
      case ChooseP(predicate, _, _) :: _ => predicate
      case HasP(_, predicate) :: _       => predicate
      case Is(predicate) :: _            => predicate
      case WhereP(predicate) :: _        => predicate
    })(steps)
      .flatMap({
        case _: StartsWith => Some("starsWith")
        case _: EndsWith   => Some("endsWith")
        case _: Contains   => Some("contains")
        case _             => None
      })

    functions ++ predicates
  }
}
