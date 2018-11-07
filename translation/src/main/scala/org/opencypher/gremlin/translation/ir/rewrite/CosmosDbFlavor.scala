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

import org.apache.tinkerpop.gremlin.process.traversal.{Order, Scope}
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.translation.traversal.DeprecatedOrderAccessor.{decr, incr}

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
      stringIds(_),
      tinkerPop334Workaround(_)
    ).foldLeft(steps) { (steps, rewriter) =>
      mapTraversals(rewriter)(steps)
    }
  }

  private def rewriteValues(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Values(propertyKeys @ _*) :: rest => Properties() :: HasKey(propertyKeys: _*) :: Value :: rest
    })(steps)
  }

  val rangeStepExpression = "\\(_ - [0-9]+\\) \\% ([0-9]+)".r

  private def rewriteRange(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case Repeat(SideEffect(aggregation) :: Nil) :: Until(untilTraversal) :: SelectK(_) :: rest =>
        (aggregation, untilTraversal) match {
          case (Loops :: Is(Gte(start: Long)) :: Aggregate(_) :: Nil, Loops :: Is(Gt(end: Long)) :: Nil) =>
            val range = start until (end + 1)
            Inject(range: _*) :: rest
          case (
              Loops :: Is(Gte(start: Long)) :: WhereT(Math(expr) :: Is(_) :: Nil) :: Aggregate(_) :: Nil,
              Loops :: Is(Gt(end: Long)) :: Nil) =>
            val step = expr match {
              case rangeStepExpression(s) => s.toLong
            }
            val range = start until (end + 1) by step
            Inject(range: _*) :: rest
          case _ => throw new IllegalArgumentException("Ranges with expressions are not supported in Cosmos Db")
        }
    })(steps)
  }

  private def tinkerPop334Workaround(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    replace({
      case By(traversal, Some(Order.asc)) :: rest =>
        By(traversal, Some(incr)) :: rest
      case By(traversal, Some(Order.desc)) :: rest =>
        By(traversal, Some(decr)) :: rest

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
      case PropertyVC(Cardinality.single, "id", value) :: rest =>
        PropertyVC(Cardinality.single, "id", "" + value) :: rest
      case PropertyTC(Cardinality.single, "id", Constant(value) :: Nil) :: rest =>
        PropertyTC(Cardinality.single, "id", Constant("" + value) :: Nil) :: rest
    })(steps)
  }
}
