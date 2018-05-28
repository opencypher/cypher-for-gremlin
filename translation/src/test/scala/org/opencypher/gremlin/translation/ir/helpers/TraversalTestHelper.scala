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
package org.opencypher.gremlin.translation.ir.helpers

import org.opencypher.gremlin.translation.ir.model.{
  And,
  By,
  ChooseP,
  ChooseT,
  Coalesce,
  GremlinStep,
  Local,
  MapT,
  Not,
  Optional,
  Or,
  Project,
  PropertyT,
  Repeat,
  SideEffect,
  Union,
  Until,
  WhereT
}

object TraversalTestHelper {

  def beforeReturn(steps: Seq[GremlinStep]): Seq[GremlinStep] = steps match {
    case MapT(Project(_*) :: By(_, _) :: _) :: Nil              => Nil
    case Project(_*) :: By(_, _) :: By(_, _) :: By(_, _) :: Nil => Nil
    case Project(_*) :: By(_, _) :: By(_, _) :: Nil             => Nil
    case Project(_*) :: By(_, _) :: Nil                         => Nil
    case s :: rest                                              => List(s) ++ beforeReturn(rest)
    case _                                                      => Nil
  }

  def containsStepsInArgs(steps: Seq[Seq[GremlinStep]], subSteps: Seq[GremlinStep]): Boolean = {
    for (arg <- steps) {
      if (containsSteps(arg, subSteps)) {
        return true
      }
    }
    return false
  }

  def containsSteps(steps: Seq[GremlinStep], subSteps: Seq[GremlinStep]): Boolean = {
    if (steps.containsSlice(subSteps)) {
      true
    } else {
      for (step <- steps) {
        if (step match {
              case And(args @ _*)          => containsStepsInArgs(args, subSteps)
              case By(traversal, _)        => containsSteps(traversal, subSteps)
              case ChooseT(_, a, b)        => containsSteps(a, subSteps) || containsSteps(b, subSteps)
              case ChooseP(_, a, b)        => containsSteps(a, subSteps) || containsSteps(b, subSteps)
              case Coalesce(args @ _*)     => containsStepsInArgs(args, subSteps)
              case Local(traversal)        => containsSteps(traversal, subSteps)
              case MapT(traversal)         => containsSteps(traversal, subSteps)
              case Not(traversal)          => containsSteps(traversal, subSteps)
              case Optional(traversal)     => containsSteps(traversal, subSteps)
              case Or(args @ _*)           => containsStepsInArgs(args, subSteps)
              case PropertyT(_, traversal) => containsSteps(traversal, subSteps)
              case Repeat(traversal)       => containsSteps(traversal, subSteps)
              case SideEffect(traversal)   => containsSteps(traversal, subSteps)
              case Union(traversal)        => containsSteps(traversal, subSteps)
              case Until(traversal)        => containsSteps(traversal, subSteps)
              case WhereT(traversal)       => containsSteps(traversal, subSteps)
              case _                       => false
            }) {
          return true
        }
      }
      false
    }
  }
}
