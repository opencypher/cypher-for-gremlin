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

import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

import scala.collection.mutable

/**
  * This rewriter relocates label and property predicates from normalized `WHERE` expressions
  * to the related `as` step as `has` steps.
  * This should allow Gremlin provider optimization strategies
  * to fold generated `has` steps into the adjacent vertex step.
  */
object GroupStepFilters extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    split(AfterStep, {
      case FlatMapT(Project(_*) :: _) => true
      case Project(_*)                => true
      case _                          => false
    })(steps)
      .flatMap(mapTraversals(rewriteSegment))
  }

  private def rewriteSegment(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val rewrittenStepLabels = extract({
      case prev :: As(stepLabel) :: _ if prev != AddV => stepLabel
      case Repeat(_ :: As(stepLabel) :: _) :: _       => stepLabel
    })(steps).toSet

    if (rewrittenStepLabels.isEmpty) {
      // No applicable step labels found
      return steps
    }

    // Group "has" steps by related step label
    val hasSteps = new mutable.HashMap[String, mutable.Set[GremlinStep]] with mutable.MultiMap[String, GremlinStep]
    val sortedHasSteps: String => List[GremlinStep] = { stepLabel =>
      hasSteps(stepLabel).toList.sortBy { // Reorder "has" steps by priority
        case _: HasLabel => 0
        case _           => 1
      }
    }

    extract({
      case WhereT(And(andTraversals @ _*) :: Nil) :: _ =>
        whereExtractor(andTraversals)
      case WhereT(whereTraversal) :: _ =>
        whereExtractor(whereTraversal :: Nil)
    })(steps).flatten.filter {
      case (stepLabel, _) => rewrittenStepLabels.contains(stepLabel)
    }.foreach {
      case (stepLabel, step) => hasSteps.addBinding(stepLabel, step)
    }

    val aliasesWhereFilter = whereFilter(rewrittenStepLabels) _
    val replaceInSteps = replace({
      case As(stepLabel) :: rest if hasSteps.contains(stepLabel) =>
        val steps = sortedHasSteps(stepLabel)
        As(stepLabel) +: (steps ++ rest)
      case Repeat(head :: As(stepLabel) :: repeatRest) :: rest if hasSteps.contains(stepLabel) =>
        val steps = sortedHasSteps(stepLabel)
        Repeat(head :: As(stepLabel) +: (steps ++ repeatRest)) :: rest
      case WhereT(And(andTraversals @ _*) :: Nil) :: rest =>
        aliasesWhereFilter(andTraversals)
          .map(_ :: rest)
          .getOrElse(rest)
      case WhereT(whereTraversal) :: rest =>
        aliasesWhereFilter(whereTraversal :: Nil)
          .map(_ :: rest)
          .getOrElse(rest)
    }) _

    val firstPass = replaceInSteps(steps)
    val secondPass = replaceInSteps(firstPass)

    secondPass
  }

  // Extracts "has" steps from a list of WHERE expressions
  private def whereExtractor(traversals: Seq[Seq[GremlinStep]]): Seq[(String, GremlinStep)] = {
    traversals.flatMap {
      case SelectK(stepLabel) :: ChooseT3(Values(propertyKey) :: Nil, Values(_) :: Nil, Constant(NULL) :: Nil)
            :: ChooseP2(Neq(NULL), Is(predicate) :: Nil) :: Is(Neq(NULL)) :: Nil =>
        (stepLabel, HasP(propertyKey, predicate)) :: Nil
      case SelectK(stepLabel) :: rest if rest.dropRight(1).forall(_.isInstanceOf[HasLabel]) =>
        rest.map((stepLabel, _))
      case _ =>
        Nil
    }
  }

  // Filters out relocated expressions from WHERE
  private def whereFilter(aliases: Set[String])(traversals: Seq[Seq[GremlinStep]]): Option[GremlinStep] = {
    val newTraversals = traversals.flatMap {
      case SelectK(alias) :: ChooseT3(Values(_) :: Nil, Values(_) :: Nil, Constant(NULL) :: Nil)
            :: ChooseP2(Neq(NULL), Is(_) :: Nil) :: Is(Neq(NULL)) :: Nil if aliases.contains(alias) =>
        None
      case SelectK(alias) :: rest if aliases.contains(alias) && rest.dropRight(1).forall(_.isInstanceOf[HasLabel]) =>
        None
      case other =>
        Some(other)
    }.toList
    newTraversals match {
      case Nil              => None
      case traversal :: Nil => Some(WhereT(traversal))
      case _                => Some(WhereT(And(newTraversals: _*) :: Nil))
    }
  }
}
