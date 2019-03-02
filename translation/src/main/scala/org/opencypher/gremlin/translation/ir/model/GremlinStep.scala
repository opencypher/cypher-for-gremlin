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
package org.opencypher.gremlin.translation.ir.model

import org.apache.tinkerpop.gremlin.process.traversal.{Pop, Scope, Order => TraversalOrder}
import org.apache.tinkerpop.gremlin.structure.Column
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality
import org.opencypher.gremlin.traversal.CustomFunction

sealed trait GremlinStep {

  /**
    * Recreates the step with all traversal arguments mapped with the provided function, if any.
    *
    * @param f mapping function
    * @return step
    */
  def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = this

  /**
    * Folds all traversal arguments from left to right with the provided operator, if any.
    *
    * @param z  start value
    * @param op folding operator
    * @tparam R folding result type
    * @return folding result
    */
  def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = z
}

case object Vertex extends GremlinStep

case object Edge extends GremlinStep

case class AddE(edgeLabel: String) extends GremlinStep

case object AddV extends GremlinStep

case class AddV(vertexLabel: String) extends GremlinStep

case class Aggregate(sideEffectKey: String) extends GremlinStep

case class And(andTraversals: Seq[GremlinStep]*) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    And(andTraversals.map(f): _*)
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    andTraversals.foldLeft(z)(op)
  }
}

case class As(stepLabel: String) extends GremlinStep

case object Barrier extends GremlinStep

case class BothE(edgeLabels: String*) extends GremlinStep

case class By(traversal: Seq[GremlinStep], order: Option[TraversalOrder] = None) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    By(f(traversal), order)
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case class Cap(sideEffectKey: String) extends GremlinStep

case class ChooseT1(choiceTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    ChooseT1(f(choiceTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, choiceTraversal)
  }
}

case class ChooseT2(choiceTraversal: Seq[GremlinStep], trueChoice: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    ChooseT2(f(choiceTraversal), f(trueChoice))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(op(z, choiceTraversal), trueChoice)
  }
}

case class ChooseT3(traversalPredicate: Seq[GremlinStep], trueChoice: Seq[GremlinStep], falseChoice: Seq[GremlinStep])
    extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    ChooseT3(f(traversalPredicate), f(trueChoice), f(falseChoice))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(op(op(z, traversalPredicate), trueChoice), falseChoice)
  }
}

case class ChooseP2(predicate: GremlinPredicate, trueChoice: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    ChooseP2(predicate, f(trueChoice))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, trueChoice)
  }
}

case class ChooseP3(predicate: GremlinPredicate, trueChoice: Seq[GremlinStep], falseChoice: Seq[GremlinStep])
    extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    ChooseP3(predicate, f(trueChoice), f(falseChoice))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(op(z, trueChoice), falseChoice)
  }
}

case class Coalesce(coalesceTraversals: Seq[GremlinStep]*) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Coalesce(coalesceTraversals.map(f): _*)
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    coalesceTraversals.foldLeft(z)(op)
  }
}

case class Constant(e: Any) extends GremlinStep

case object Count extends GremlinStep

case class CountS(scope: Scope) extends GremlinStep

case class Dedup(dedupLabels: String*) extends GremlinStep

case object Drop extends GremlinStep

case object Emit extends GremlinStep

case class EmitT(traversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    EmitT(f(traversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case class FlatMapT(traversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    FlatMapT(f(traversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case object Fold extends GremlinStep

case class From(fromStepLabel: String) extends GremlinStep

case object Group extends GremlinStep

case class Has(propertyKey: String) extends GremlinStep

case class HasP(propertyKey: String, predicate: GremlinPredicate) extends GremlinStep

case class HasKey(labels: String*) extends GremlinStep

case class HasLabel(labels: String*) extends GremlinStep

case class HasNot(propertyKey: String) extends GremlinStep

case object Id extends GremlinStep

case object Identity extends GremlinStep

case class InE(edgeLabels: String*) extends GremlinStep

case object InV extends GremlinStep

case class Inject(injections: Any*) extends GremlinStep

case class Is(predicate: GremlinPredicate) extends GremlinStep

case object Key extends GremlinStep

case object Label extends GremlinStep

case class Limit(limit: Long) extends GremlinStep

case class LimitS(scope: Scope, limit: Long) extends GremlinStep

case class Local(traversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Local(f(traversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case object Loops extends GremlinStep

case class MapF(function: CustomFunction) extends GremlinStep

case class MapT(traversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    MapT(f(traversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case class Math(expression: String) extends GremlinStep

case object Max extends GremlinStep

case object Mean extends GremlinStep

case object Min extends GremlinStep

case class Not(notTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Not(f(notTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, notTraversal)
  }
}

case class OptionT(pickToken: Object, traversalOption: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    OptionT(pickToken, f(traversalOption))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversalOption)
  }
}

case class Optional(optionalTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Optional(f(optionalTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, optionalTraversal)
  }
}

case class Or(orTraversals: Seq[GremlinStep]*) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Or(orTraversals.map(f): _*)
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    orTraversals.foldLeft(z)(op)
  }
}

case object Order extends GremlinStep

case object OtherV extends GremlinStep

case class OutE(edgeLabels: String*) extends GremlinStep

case object OutV extends GremlinStep

case object Path extends GremlinStep

case class Properties(propertyKeys: String*) extends GremlinStep

case class PropertyG(token: org.apache.tinkerpop.gremlin.structure.T, value: Any) extends GremlinStep

case class PropertyV(key: String, value: Any) extends GremlinStep

case class PropertyVC(cardinality: Cardinality, key: String, value: Any) extends GremlinStep

case class PropertyT(key: String, traversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    PropertyT(key, f(traversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case class PropertyTC(cardinality: Cardinality, key: String, traversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    PropertyTC(cardinality, key, f(traversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, traversal)
  }
}

case class Project(keys: String*) extends GremlinStep

case class Range(scope: Scope, low: Long, high: Long) extends GremlinStep

case class Repeat(repeatTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Repeat(f(repeatTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, repeatTraversal)
  }
}

case class SelectP(pop: Pop, selectKey: String) extends GremlinStep

case class SelectK(selectKeys: String*) extends GremlinStep

case class SelectC(column: Column) extends GremlinStep

case class SideEffect(sideEffectTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    SideEffect(f(sideEffectTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, sideEffectTraversal)
  }
}

case object SimplePath extends GremlinStep

case class Skip(skip: Long) extends GremlinStep

case object Sum extends GremlinStep

case class Times(maxLoops: Int) extends GremlinStep

case class Tail(scope: Scope, limit: Long) extends GremlinStep

case class To(toStepLabel: String) extends GremlinStep

case object Unfold extends GremlinStep

case class Union(unionTraversals: Seq[GremlinStep]*) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Union(unionTraversals.map(f): _*)
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    unionTraversals.foldLeft(z)(op)
  }
}

case class Until(untilTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    Until(f(untilTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, untilTraversal)
  }
}

case object Value extends GremlinStep

case object ValueMap extends GremlinStep

case class ValueMap(includeTokens: Boolean) extends GremlinStep

case class Values(propertyKeys: String*) extends GremlinStep

case class WhereT(whereTraversal: Seq[GremlinStep]) extends GremlinStep {
  override def mapTraversals(f: Seq[GremlinStep] => Seq[GremlinStep]): GremlinStep = {
    WhereT(f(whereTraversal))
  }

  override def foldTraversals[R](z: R)(op: (R, Seq[GremlinStep]) => R): R = {
    op(z, whereTraversal)
  }
}

case class WhereP(predicate: GremlinPredicate) extends GremlinStep
