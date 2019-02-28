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
package org.opencypher.gremlin.translation.ir

import org.opencypher.gremlin.translation.ir.model.GremlinStep

import scala.annotation.tailrec
import scala.language.implicitConversions

/**
  * Gremlin IR manipulation utilities
  */
object TraversalHelper {

  /**
    * Maps top-level and all nested traversals starting from the bottom and going up.
    *
    * @param f     mapping function
    * @param steps top-level traversal
    * @return mapping result
    */
  def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep])(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    f(steps.map({ step =>
      step.mapTraversals(steps => mapTraversals(f)(steps))
    }))
  }

  /**
    * Folds top-level and all nested traversals starting from the top and going down.
    *
    * @param z     start value
    * @param op    folding operator
    * @param steps top-level traversal
    * @tparam R folding result type
    * @return folding result
    */
  def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R)(steps: Seq[GremlinStep]): R = {
    steps.foldLeft(op(z, steps)) { (acc, step) =>
      step.foldTraversals(acc)(foldTraversals(_)(op)(_))
    }
  }

  /**
    * Finds matching parts of an IR sequence and maps occurrences.
    *
    * @param extractor matching and mapping function
    * @param steps     IR sequence
    * @tparam R mapping result
    * @return list of extracted values
    */
  def extract[R](extractor: PartialFunction[Seq[GremlinStep], R])(steps: Seq[GremlinStep]): Seq[R] = {
    @tailrec def findAcc(acc: Seq[R], steps: Seq[GremlinStep]): Seq[R] = {
      steps match {
        case _ :: tail if extractor.isDefinedAt(steps) =>
          val found = extractor(steps)
          findAcc(acc :+ found, tail)
        case _ :: tail =>
          findAcc(acc, tail)
        case Nil =>
          acc
      }
    }

    findAcc(Nil, steps)
  }

  /**
    * Count occurrences in top-level and all nested traversals starting from the bottom and going up.
    *
    * @param extractor matching and mapping function
    * @param steps top-level traversal
    * @return map with number of occurrences of extractor result
    */
  def countInTraversals[R](extractor: PartialFunction[Seq[GremlinStep], R])(steps: Seq[GremlinStep]): Map[R, Int] = {
    foldTraversals(Map.empty[R, Int])({ (acc, sub) =>
      val labels = extract(extractor)(sub)
      labels.foldLeft(acc) { (acc, label) =>
        acc + ((label, acc.getOrElse(label, 0) + 1))
      }
    })(steps)
  }

  /**
    * Finds matching parts of an IR sequence and replaces them.
    *
    * @param steps    IR sequence
    * @param replacer matching and replacing function
    * @return rewritten IR sequence
    */
  def replace(replacer: PartialFunction[Seq[GremlinStep], Seq[GremlinStep]])(
      steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    @tailrec def replaceAcc(acc: Seq[GremlinStep], steps: Seq[GremlinStep]): Seq[GremlinStep] = {
      steps match {
        case _ :: _ if replacer.isDefinedAt(steps) =>
          val replaced = replacer(steps)
          replaced.headOption match {
            case Some(head) => replaceAcc(acc :+ head, replaced.tail)
            case _          => acc
          }
        case head :: tail =>
          replaceAcc(acc :+ head, tail)
        case Nil =>
          acc
      }
    }

    replaceAcc(Nil, steps)
  }

  /**
    * Finds matching steps in the IR sequence and splits the sequence in segments,
    * with each matching step marking the end of a segment.
    *
    * @param steps    IR sequence
    * @param splitter matching function
    * @return IR sequence segments
    */
  def split(splitMode: SplitMode, splitter: GremlinStep => Boolean)(steps: Seq[GremlinStep]): Seq[Seq[GremlinStep]] = {
    @tailrec def splitAcc(
        acc: Seq[Seq[GremlinStep]],
        current: Seq[GremlinStep],
        rest: Seq[GremlinStep]): Seq[Seq[GremlinStep]] = {
      rest match {
        case step :: _ =>
          if (splitter(step)) {
            val segment = splitMode match {
              case BeforeStep => current
              case AfterStep  => current :+ step
            }
            val nextAcc = segment match {
              case Nil => acc
              case _   => acc :+ segment
            }
            val next = splitMode match {
              case BeforeStep => step :: Nil
              case AfterStep  => Nil
            }
            splitAcc(nextAcc, next, rest.tail)
          } else {
            splitAcc(acc, current :+ step, rest.tail)
          }
        case Nil =>
          if (current.nonEmpty) acc :+ current else acc
      }
    }

    splitAcc(Nil, Nil, steps)
  }

  sealed trait SplitMode
  case object BeforeStep extends SplitMode
  case object AfterStep extends SplitMode
}
