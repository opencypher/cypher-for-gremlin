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

import org.opencypher.gremlin.translation.ir.model.GremlinStep

import scala.annotation.tailrec

/**
  * Gremlin IR rewriting utilities
  */
object Rewriting {

  /**
    * Finds matching parts of an IR sequence and maps occurrences.
    *
    * @param steps IR sequence
    * @param finder matching and mapping function
    * @tparam R mapping result
    * @return list of extracted values
    */
  def extract[R](steps: Seq[GremlinStep], finder: PartialFunction[Seq[GremlinStep], R]): Seq[R] = {
    @tailrec def findAcc(acc: Seq[R], steps: Seq[GremlinStep]): Seq[R] = {
      steps match {
        case _ :: tail if finder.isDefinedAt(steps) =>
          val found = finder(steps)
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
    * Finds matching parts of an IR sequence and replaces them.
    *
    * @param steps IR sequence
    * @param replacer matching and replacing function
    * @return rewritten IR sequence
    */
  def replace(
      steps: Seq[GremlinStep],
      replacer: PartialFunction[Seq[GremlinStep], Seq[GremlinStep]]): Seq[GremlinStep] = {
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
    * @param steps IR sequence
    * @param splitter matching function
    * @return IR sequence segments
    */
  def splitAfter(steps: Seq[GremlinStep], splitter: GremlinStep => Boolean): Seq[Seq[GremlinStep]] = {
    @tailrec def splitAfterAcc(
        acc: Seq[Seq[GremlinStep]],
        current: Seq[GremlinStep],
        rest: Seq[GremlinStep]): Seq[Seq[GremlinStep]] = {
      rest match {
        case step :: _ =>
          val next = current :+ step
          if (splitter(step)) {
            splitAfterAcc(acc :+ next, Nil, rest.tail)
          } else {
            splitAfterAcc(acc, next, rest.tail)
          }
        case Nil =>
          if (current.nonEmpty) acc :+ current else acc
      }
    }
    splitAfterAcc(Nil, Nil, steps)
  }
}
