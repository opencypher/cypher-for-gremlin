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

import org.apache.tinkerpop.gremlin.process.traversal.step.map.MathStepAccessor
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

import scala.collection.JavaConverters._
import scala.collection.SortedMap

/**
  * This rewriter removes many cases of `as` steps that have been generated,
  * but are not actually used in the traversal.
  * This enables some default Gremlin optimization strategies like
  * [[org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.IncidentToAdjacentStrategy]]
  * and allows bulking by keeping traversers compact.
  */
object RemoveUnusedAliases extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val selected = foldTraversals(SortedMap.empty[String, Int])((acc, localSteps) => {
      def increment(keys: String*): SortedMap[String, Int] = {
        keys.foldLeft(acc)((acc, key) => acc.updated(key, acc.getOrElse(key, 0) + 1))
      }

      acc ++ extract({
        case From(fromStepLabel) :: _      => increment(fromStepLabel)
        case To(toStepLabel) :: _          => increment(toStepLabel)
        case SelectK(selectKeys @ _*) :: _ => increment(selectKeys: _*)
        case Dedup(dedupLabels @ _*) :: _  => increment(dedupLabels: _*)
        case WhereP(predicate) :: _        => increment(predicateAliases(predicate): _*)
        case Math(expression) :: _         => increment(MathStepAccessor.getVariables(expression).asScala.toSeq: _*)
      })(localSteps).flatten
    })(steps)

    mapTraversals(traversal =>
      traversal.flatMap {
        case As(stepLabel) if !selected.contains(stepLabel) => None
        case s                                              => Some(s)
    })(steps)
  }

  def predicateAliases(predicate: GremlinPredicate): Seq[String] = {
    def strings(values: Any*): Seq[String] = {
      values
        .filter(_.isInstanceOf[String])
        .map(_.asInstanceOf[String])
    }

    predicate match {
      case Eq(value)              => strings(value)
      case Gt(value)              => strings(value)
      case Gte(value)             => strings(value)
      case Lt(value)              => strings(value)
      case Lte(value)             => strings(value)
      case Neq(value)             => strings(value)
      case Between(first, second) => strings(first, second)
      case Within(values @ _*)    => strings(values: _*)
      case Without(values @ _*)   => strings(values: _*)
      case StartsWith(value)      => strings(value)
      case EndsWith(value)        => strings(value)
      case Contains(value)        => strings(value)
      case IsNode()               => Seq()
      case IsString()             => Seq()
    }
  }
}
