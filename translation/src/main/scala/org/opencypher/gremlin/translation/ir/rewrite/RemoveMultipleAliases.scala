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
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * This rule replaces multiple sequential step aliases with single one, and updates traversal accordingly
  */
object RemoveMultipleAliases extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val allAliases = foldTraversals(Seq.empty[String])({ (acc, sub) =>
      acc ++ extract({
        case As(label) :: _ => label
      })(sub)
    })(steps)

    val replaceAliases = foldTraversals(Map.empty[String, String])({ (acc, sub) =>
      acc ++ getAliasReplacements(sub, allAliases)
    })(steps)

    def alias(alias: String) = {
      replaceAliases.getOrElse(alias, alias)
    }

    mapTraversals(traversal =>
      traversal.flatMap {
        case As(label) if replaceAliases.contains(label) =>
          Nil
        case From(fromStepLabel) =>
          Seq(From(alias(fromStepLabel)))
        case To(toStepLabel) =>
          Seq(To(alias(toStepLabel)))
        case SelectK(selectKeys @ _*) =>
          Seq(SelectK(selectKeys.map(alias): _*))
        case Dedup(dedupLabels @ _*) =>
          Seq(Dedup(dedupLabels.map(alias): _*))
        case WhereP(predicate) =>
          Seq(WhereP(replacePredicate(predicate, alias)))
        case Math(expression) =>
          Seq(Math(replaceMath(expression, alias)))
        case s =>
          Seq(s)
    })(steps)
  }

  private def getAliasReplacements(steps: Seq[GremlinStep], allAliases: Seq[String]) = {
    val replacements = mutable.Map.empty[String, String]
    var stepBuf = mutable.ArrayBuffer.empty[String]

    def add = if (stepBuf.size > 1) {
      stepBuf.tail.foreach(replacements(_) = stepBuf.head)
    }

    steps.foreach {
      case As(label) if label != UNUSED && allAliases.count(_ == label) == 1 =>
        stepBuf += label
      case _ =>
        add
        stepBuf.clear()
    }
    add
    replacements
  }

  private def replacePredicate(predicate: GremlinPredicate, alias: String => String): GremlinPredicate =
    predicate match {
      case Eq(value: String)                      => Eq(alias(value))
      case Gt(value: String)                      => Gt(alias(value))
      case Gte(value: String)                     => Gte(alias(value))
      case Lt(value: String)                      => Lt(alias(value))
      case Lte(value: String)                     => Lte(alias(value))
      case Neq(value: String)                     => Neq(alias(value))
      case Between(first: String, second: String) => Between(alias(first), alias(second))
      case Within(values @ _*)                    => Within(aliasCollection(alias, values): _*)
      case Without(values @ _*)                   => Without(aliasCollection(alias, values): _*)
      case StartsWith(value: String)              => StartsWith(alias(value))
      case EndsWith(value: String)                => EndsWith(alias(value))
      case Contains(value: String)                => Contains(alias(value))
      case _                                      => throw new IllegalArgumentException("Unknown predicate " + predicate)
    }

  private def aliasCollection(alias: String => String, values: Seq[Any]) = {
    values.headOption match {
      case Some(_: String) => values.asInstanceOf[Seq[String]].map(alias)
      case _               => values
    }
  }

  private def replaceMath(expression: String, alias: String => String): String =
    MathStepAccessor
      .getVariables(expression)
      .asScala
      .foldLeft(expression)((s, v) => s.replaceAll(v, alias(v)))
}
