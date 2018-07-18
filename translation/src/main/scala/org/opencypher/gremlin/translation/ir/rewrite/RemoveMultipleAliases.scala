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

/**
  * This rule replaces multiple sequential step aliases with single one, and updates traversal accordingly
  */
object RemoveMultipleAliases extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val aliasCount = foldTraversals(Map.empty[String, Int])({ (acc, sub) =>
      val labels = extract({
        case As(label) :: _ => label
      })(sub)
      labels.foldLeft(acc) { (acc, label) =>
        acc + ((label, acc.getOrElse(label, 0) + 1))
      }
    })(steps)

    val replaceAliases = foldTraversals(Map.empty[String, String])({ (acc, sub) =>
      acc ++ getAliasReplacements(sub, aliasCount)
    })(steps)

    def alias(alias: String) = {
      replaceAliases.getOrElse(alias, alias)
    }

    mapTraversals(traversal =>
      traversal.flatMap {
        case As(label) if replaceAliases.contains(label) =>
          None
        case From(fromStepLabel) =>
          Some(From(alias(fromStepLabel)))
        case To(toStepLabel) =>
          Some(To(alias(toStepLabel)))
        case SelectK(selectKeys @ _*) =>
          Some(SelectK(selectKeys.map(alias): _*))
        case Dedup(dedupLabels @ _*) =>
          Some(Dedup(dedupLabels.map(alias): _*))
        case WhereP(predicate) =>
          Some(WhereP(replacePredicate(predicate, alias)))
        case Math(expression) =>
          Some(Math(replaceMath(expression, alias)))
        case s =>
          Some(s)
    })(steps)
  }

  private def getAliasReplacements(steps: Seq[GremlinStep], aliasCount: Map[String, Int]): Map[String, String] = {
    def isAs: PartialFunction[GremlinStep, Boolean] = {
      case As(label) if label != UNUSED && aliasCount(label) == 1 =>
        true
      case _ =>
        false
    }

    def extractAsStepSpans(acc: Seq[Seq[String]], steps: Seq[GremlinStep]): Seq[Seq[String]] = {
      val (_, asStepsPrefix) = steps.span(!isAs(_))
      if (asStepsPrefix.isEmpty) {
        return acc
      }
      val (current, rest) = asStepsPrefix.span(isAs)
      val labels = current.map(_.asInstanceOf[As].stepLabel)
      extractAsStepSpans(acc :+ labels, rest)
    }

    extractAsStepSpans(Nil, steps)
      .filter(_.length > 1)
      .foldLeft(Map.empty[String, String]) { (acc, labels) =>
        val userDefinedFirst = labels
          .sortWith((_, k) => isGenerated(k))
        val replacement = userDefinedFirst.head
        val pairs = userDefinedFirst.tail.map((_, replacement))
        acc ++ pairs
      }
  }

  private def isGenerated(k: String): Boolean =
    Set(FRESHID, UNNAMED, PATH_START, MATCH_START, MATCH_END, GENERATED).exists(k.startsWith)

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

  private def aliasCollection(alias: String => String, values: Seq[Any]): Seq[Any] = {
    values.map {
      case v: String => alias(v)
      case v         => v
    }
  }

  private def replaceMath(expression: String, alias: String => String): String =
    MathStepAccessor
      .getVariables(expression)
      .asScala
      .foldLeft(expression)((s, v) => s.replaceAll(v, alias(v)))
}
