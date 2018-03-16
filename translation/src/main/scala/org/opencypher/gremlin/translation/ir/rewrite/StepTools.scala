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

import org.opencypher.gremlin.translation.ir.GremlinStep

import scala.annotation.tailrec

object StepTools {
  def find[R](steps: Seq[GremlinStep], finder: PartialFunction[Seq[GremlinStep], R]): Seq[R] = {
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

  def replace(
      steps: Seq[GremlinStep],
      replacer: PartialFunction[Seq[GremlinStep], Seq[GremlinStep]]): Seq[GremlinStep] = {
    @tailrec def replaceAcc(acc: Seq[GremlinStep], steps: Seq[GremlinStep]): Seq[GremlinStep] = {
      steps match {
        case _ :: _ if replacer.isDefinedAt(steps) =>
          val replaced = replacer(steps)
          replaceAcc(acc :+ replaced.head, replaced.tail)
        case head :: tail =>
          replaceAcc(acc :+ head, tail)
        case Nil =>
          acc
      }
    }
    replaceAcc(Nil, steps)
  }
}
