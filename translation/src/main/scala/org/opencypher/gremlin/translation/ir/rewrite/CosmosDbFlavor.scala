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

import org.apache.tinkerpop.gremlin.process.traversal.Scope
import org.apache.tinkerpop.gremlin.structure.Column
import org.opencypher.gremlin.translation.Tokens
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * This is a set of rewrites to adapt the translation to Cosmos DB.
  */
object CosmosDbFlavor extends GremlinRewriter {
  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    Seq(
      rewriteValues(_),
      rewriteRange(_),
      rewriteChoose(_),
      rewriteSkip(_),
      removeFromTo(_),
      replaceSelectValues(_),
      replaceSelectValues(_),
      stringIds(_),
      neqOnDiff(_)
    ).foldLeft(steps) { (steps, rewriter) =>
      mapTraversals(rewriter)(steps)
    }
  }

  private def rewriteValues(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Values(propertyKeys @ _*) :: rest => Properties() :: HasKey(propertyKeys: _*) :: Value :: rest
    })(steps)
  }

  private def removeFromTo(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Path :: From(_) :: To(_) :: rest       => Path :: rest
      case Path :: From(_) :: rest                => Path :: rest
      case SimplePath :: From(_) :: To(_) :: rest => SimplePath :: rest
      case SimplePath :: From(_) :: rest          => SimplePath :: rest
    })(steps)
  }

  /**
    * g.inject(1).project('a').project('b').unfold().select(values).select('a') - not work
    * g.inject(1).project('a').project('b').select(values).unfold().select('a') - works
    */
  private def replaceSelectValues(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Unfold :: SelectC(Column.values) :: rest => SelectC(Column.values) :: Unfold :: rest
    })(steps)
  }

  val rangeStepExpression = "\\(_ - [0-9]+\\) \\% ([0-9]+)".r

  private def rewriteRange(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    def range(aggregation: Seq[GremlinStep], untilTraversal: Seq[GremlinStep], rest: List[GremlinStep]) = {
      (aggregation, untilTraversal) match {
        case (Loops :: Is(Gte(start: Long)) :: Aggregate(_) :: Nil, Loops :: Is(Gt(end: Long)) :: Nil) =>
          val range = (start until (end + 1)).reverse
          Inject(range: _*) :: rest
        case (
            Loops :: Is(Gte(start: Long)) :: WhereT(Math(expr) :: Is(_) :: Nil) :: Aggregate(_) :: Nil,
            Loops :: Is(Gt(end: Long)) :: Nil) =>
          val step = expr match {
            case rangeStepExpression(s) => s.toLong
          }
          val range = (start until (end + 1) by step).reverse
          Inject(range: _*) :: rest
        case _ => throw new IllegalArgumentException("Ranges with expressions are not supported in Cosmos Db")
      }
    }

    replace({
      case Repeat(SideEffect(aggregation) :: Nil) :: Until(untilTraversal) :: SelectK(_) :: rest =>
        range(aggregation, untilTraversal, rest)
      case Inject(Tokens.START) :: Repeat(SideEffect(aggregation) :: Nil) :: Until(untilTraversal) :: SelectK(_) :: rest =>
        range(aggregation, untilTraversal, rest)
    })(steps)
  }

  private def rewriteChoose(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case ChooseP2(predicate, trueChoice) :: rest =>
        ChooseP3(predicate, trueChoice, Identity :: Nil) :: rest
    })(steps)
  }

  private def rewriteSkip(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Skip(skip) :: rest =>
        Range(Scope.global, skip, Int.MaxValue) :: rest
    })(steps)
  }

  private def stringIds(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case PropertyVC(single, "id", value) :: rest =>
        PropertyVC(single, "id", "" + value) :: rest
      case PropertyV("id", value) :: rest =>
        PropertyV("id", "" + value) :: rest
      case PropertyT("id", Constant(value) :: Nil) :: rest =>
        PropertyT("id", Constant("" + value) :: Nil) :: rest
      case PropertyTC(single, "id", Constant(value) :: Nil) :: rest =>
        PropertyTC(single, "id", Constant("" + value) :: Nil) :: rest
    })(steps)
  }

  private def neqOnDiff(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Is(Neq(value)) :: rest =>
        Not(Is(Eq(value)) :: Nil) :: rest
    })(steps)
  }
}
