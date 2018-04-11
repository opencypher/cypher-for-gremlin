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
package org.opencypher.gremlin.translation.ir.builder

import org.apache.tinkerpop.gremlin.process.traversal.{Scope, Order => TOrder}
import org.apache.tinkerpop.gremlin.structure.Column
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.traversal.CustomFunction

import scala.collection.mutable

class IRGremlinSteps extends GremlinSteps[Seq[GremlinStep], GremlinPredicate] {

  private val buf = mutable.ListBuffer.empty[GremlinStep]

  override def current(): Seq[GremlinStep] = buf.toList

  override def start(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = new IRGremlinSteps

  override def V(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Vertex
    this
  }

  override def addE(edgeLabel: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += AddE(edgeLabel)
    this
  }

  override def addV(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += AddV
    this
  }

  override def addV(vertexLabel: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += AddV(vertexLabel)
    this
  }

  override def aggregate(sideEffectKey: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Aggregate(sideEffectKey)
    this
  }

  override def and(andTraversals: GremlinSteps[Seq[GremlinStep], GremlinPredicate]*)
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += And(andTraversals.map(_.current): _*)
    this
  }

  override def as(stepLabel: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += As(stepLabel)
    this
  }

  override def barrier(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Barrier
    this
  }

  override def bothE(edgeLabels: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += BothE(edgeLabels: _*)
    this
  }

  override def by(
      traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += By(traversal.current(), None)
    this
  }

  override def by(
      traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate],
      order: TOrder): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += By(traversal.current(), Some(order))
    this
  }

  override def cap(sideEffectKey: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Cap(sideEffectKey)
    this
  }

  override def choose(
      traversalPredicate: GremlinSteps[Seq[GremlinStep], GremlinPredicate],
      trueChoice: GremlinSteps[Seq[GremlinStep], GremlinPredicate],
      falseChoice: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += ChooseT(traversalPredicate.current(), trueChoice.current(), falseChoice.current())
    this
  }

  override def choose(
      predicate: GremlinPredicate,
      trueChoice: GremlinSteps[Seq[GremlinStep], GremlinPredicate],
      falseChoice: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += ChooseP(predicate, trueChoice.current(), falseChoice.current())
    this
  }

  override def choose(predicate: GremlinPredicate, trueChoice: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += ChooseP(predicate, trueChoice.current(), Nil)
    this
  }

  override def coalesce(coalesceTraversals: GremlinSteps[Seq[GremlinStep], GremlinPredicate]*)
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Coalesce(coalesceTraversals.map(_.current): _*)
    this
  }

  override def constant(e: scala.Any): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Constant(e)
    this
  }

  override def count(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Count
    this
  }

  override def count(scope: Scope): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += CountS(scope)
    this
  }

  override def dedup(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Dedup
    this
  }

  override def drop(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Drop
    this
  }

  override def emit(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Emit
    this
  }

  override def fold(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Fold
    this
  }

  override def from(fromStepLabel: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += From(fromStepLabel)
    this
  }

  override def group(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Group
    this
  }

  override def has(propertyKey: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Has(propertyKey)
    this
  }

  override def has(
      propertyKey: String,
      predicate: GremlinPredicate): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += HasP(propertyKey, predicate)
    this
  }

  override def hasKey(labels: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += HasKey(labels: _*)
    this
  }

  override def hasLabel(labels: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += HasLabel(labels: _*)
    this
  }

  override def hasNot(propertyKey: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += HasNot(propertyKey)
    this
  }

  override def id(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Id
    this
  }

  override def identity(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Identity
    this
  }

  override def inE(edgeLabels: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += InE(edgeLabels: _*)
    this
  }

  override def inV(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += InV
    this
  }

  override def inject(injections: AnyRef*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Inject(injections: _*)
    this
  }

  override def is(predicate: GremlinPredicate): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Is(predicate)
    this
  }

  override def key(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Key
    this
  }

  override def label(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Label
    this
  }

  override def limit(limit: Long): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Limit(limit)
    this
  }

  override def local(
      traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Local(traversal.current())
    this
  }

  override def loops(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Loops
    this
  }

  override def map(function: CustomFunction): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += MapF(function)
    this
  }

  override def map(
      traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += MapT(traversal.current())
    this
  }

  override def math(expression: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Math(expression)
    this
  }

  override def max(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Max
    this
  }

  override def mean(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Mean
    this
  }

  override def min(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Min
    this
  }

  override def not(notTraversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Not(notTraversal.current())
    this
  }

  override def or(orTraversals: GremlinSteps[Seq[GremlinStep], GremlinPredicate]*)
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Or(orTraversals.map(_.current): _*)
    this
  }

  override def order(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Order
    this
  }

  override def otherV(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += OtherV
    this
  }

  override def outE(edgeLabels: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += OutE(edgeLabels: _*)
    this
  }

  override def outV(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += OutV
    this
  }

  override def path(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Path
    this
  }

  override def properties(propertyKeys: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Properties(propertyKeys: _*)
    this
  }

  override def property(key: String, value: scala.Any): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += PropertyV(key, value)
    this
  }

  override def property(
      key: String,
      traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += PropertyT(key, traversal.current())
    this
  }

  override def project(keys: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Project(keys: _*)
    this
  }

  override def repeat(repeatTraversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Repeat(repeatTraversal.current())
    this
  }

  override def select(selectKeys: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += SelectK(selectKeys: _*)
    this
  }

  override def select(column: Column): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += SelectC(column)
    this
  }

  override def sideEffect(sideEffectTraversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += SideEffect(sideEffectTraversal.current())
    this
  }

  override def skip(skip: Long): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Skip(skip)
    this
  }

  override def sum(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Sum
    this
  }

  override def times(maxLoops: Int): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Times(maxLoops)
    this
  }

  override def to(toStepLabel: String): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += To(toStepLabel)
    this
  }

  override def unfold(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Unfold
    this
  }

  override def union(unionTraversals: GremlinSteps[Seq[GremlinStep], GremlinPredicate]*)
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Union(unionTraversals.map(_.current): _*)
    this
  }

  override def until(untilTraversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Until(untilTraversal.current())
    this
  }

  override def value(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Value
    this
  }

  override def valueMap(): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += ValueMap
    this
  }

  override def valueMap(includeTokens: Boolean): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += ValueMap(includeTokens)
    this
  }

  override def values(propertyKeys: String*): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += Values(propertyKeys: _*)
    this
  }

  override def where(whereTraversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate])
    : GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += WhereT(whereTraversal.current())
    this
  }

  override def where(predicate: GremlinPredicate): GremlinSteps[Seq[GremlinStep], GremlinPredicate] = {
    buf += WhereP(predicate)
    this
  }
}
