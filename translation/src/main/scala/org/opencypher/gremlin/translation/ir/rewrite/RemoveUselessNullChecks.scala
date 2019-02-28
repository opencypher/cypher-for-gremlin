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
package org.opencypher.gremlin.translation.ir.rewrite

import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

import scala.annotation.tailrec

/**
  * This rule finds instances of if-not-null pattern and removes them
  * if there are no prior steps in the traversal that may produce nulls.
  */
object RemoveUselessNullChecks extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    @tailrec def splitterAcc(acc: Seq[GremlinStep], steps: Seq[GremlinStep]): Seq[GremlinStep] = {
      val (segment, rest) = splitSegment(steps)
      val rewritten = rewriteSegment(acc ++ segment)
      if (rest.nonEmpty) splitterAcc(rewritten, rest) else rewritten
    }
    splitterAcc(Nil, steps)
  }

  private def splitSegment(steps: Seq[GremlinStep]): (Seq[GremlinStep], Seq[GremlinStep]) = {
    val (segment, rest) = steps.span {
      case By(SelectK(_) :: ChooseP2(Neq(NULL), _) :: Nil, None) => false
      case By(ChooseP2(Neq(NULL), _) :: Nil, None)               => false
      case ChooseP2(Neq(NULL), _)                                => false
      case _                                                     => true
    }
    rest match {
      case head :: tail => (segment :+ head, tail)
      case _            => (segment, Nil)
    }
  }

  private def rewriteSegment(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    val mapsToNull = foldTraversals(false)({ (acc, steps) =>
      acc || steps.exists {
        case MapF(_)        => true
        case Constant(NULL) => true
        case _              => false
      }
    })(steps.init)

    if (mapsToNull) {
      return steps
    }

    val last = steps.last match {
      case By(SelectK(key) :: ChooseP2(Neq(NULL), traversal) :: Nil, None) =>
        By(SelectK(key) +: traversal, None) :: Nil
      case By(ChooseP2(Neq(NULL), traversal) :: Nil, None) =>
        By(traversal, None) :: Nil
      case ChooseP2(Neq(NULL), traversal) =>
        traversal
      case step =>
        step :: Nil
    }
    steps.init ++ last
  }
}
