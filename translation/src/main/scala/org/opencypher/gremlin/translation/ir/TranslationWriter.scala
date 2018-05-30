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
package org.opencypher.gremlin.translation.ir

import org.apache.tinkerpop.gremlin.process.traversal.Scope
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.translation.ir.rewrite.GremlinRewriter
import org.opencypher.gremlin.translation.ir.verify.GremlinPostCondition
import org.opencypher.gremlin.translation.translator.Translator

object TranslationWriter {
  def from(ir: Seq[GremlinStep]): TranslationWriter = {
    new TranslationWriter(ir, Nil, Nil)
  }
}

class TranslationWriter(
    ir: Seq[GremlinStep],
    rewriters: Seq[GremlinRewriter],
    postConditions: Seq[GremlinPostCondition]) {
  def rewrite(rewriters: GremlinRewriter*): TranslationWriter =
    new TranslationWriter(ir, this.rewriters ++ rewriters, postConditions)

  def verify(postConditions: GremlinPostCondition*): TranslationWriter =
    new TranslationWriter(ir, rewriters, this.postConditions ++ postConditions)

  def translate[T, P](translator: Translator[T, P]): T = {
    val rewritten = rewriters.foldLeft(ir)((ir, rewriter) => rewriter(ir))

    postConditions
      .flatMap(postCondition => postCondition(rewritten))
      .foreach(msg => throw new SyntaxException(msg))

    val generator = new TranslationGenerator(translator)
    generator.generate(rewritten)
    translator.translate()
  }
}

sealed private[ir] class TranslationGenerator[T, P](translator: Translator[T, P]) {
  private val g = translator.steps()
  private val p = translator.predicates()
  private val b = translator.bindings()

  def generate(ir: Seq[GremlinStep]): GremlinSteps[T, P] = {
    generateSteps(ir, g)
  }

  private def generateSteps(ir: Seq[GremlinStep], g: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    for (step <- ir) {
      step match {
        case Vertex =>
          g.V()
        case AddE(edgeLabel) =>
          g.addE(edgeLabel)
        case AddV =>
          g.addV()
        case AddV(vertexLabel) =>
          g.addV(vertexLabel)
        case Aggregate(sideEffectKey) =>
          g.aggregate(sideEffectKey)
        case And(andTraversals @ _*) =>
          g.and(andTraversals.map(generateSteps): _*)
        case As(stepLabel) =>
          g.as(stepLabel)
        case Barrier =>
          g.barrier()
        case BothE(edgeLabels @ _*) =>
          g.bothE(edgeLabels: _*)
        case By(traversal, order) =>
          order
            .map(g.by(generateSteps(traversal), _))
            .getOrElse(g.by(generateSteps(traversal)))
        case Cap(sideEffectKey) =>
          g.cap(sideEffectKey)
        case ChooseT(traversalPredicate, trueChoice, falseChoice) =>
          if (trueChoice.nonEmpty && falseChoice.nonEmpty) {
            g.choose(generateSteps(traversalPredicate), generateSteps(trueChoice), generateSteps(falseChoice))
          }
        case ChooseP(predicate, trueChoice, falseChoice) =>
          if (trueChoice.nonEmpty && falseChoice.nonEmpty) {
            g.choose(generatePredicate(predicate), generateSteps(trueChoice), generateSteps(falseChoice))
          } else if (trueChoice.nonEmpty) {
            g.choose(generatePredicate(predicate), generateSteps(trueChoice))
          }
        case Coalesce(coalesceTraversals @ _*) =>
          g.coalesce(coalesceTraversals.map(generateSteps): _*)
        case Constant(e) =>
          g.constant(generateValue(e))
        case Count =>
          g.count()
        case CountS(scope) =>
          g.count(scope)
        case Dedup(dedupLabels @ _*) =>
          g.dedup(dedupLabels: _*)
        case Drop =>
          g.drop()
        case Emit =>
          g.emit()
        case Fold =>
          g.fold()
        case From(fromStepLabel) =>
          g.from(fromStepLabel)
        case Group =>
          g.group()
        case Has(propertyKey) =>
          g.has(propertyKey)
        case HasP(propertyKey, predicate) =>
          g.has(propertyKey, generatePredicate(predicate))
        case HasKey(labels @ _*) =>
          g.hasKey(labels: _*)
        case HasLabel(labels @ _*) =>
          g.hasLabel(labels: _*)
        case HasNot(propertyKey) =>
          g.hasNot(propertyKey)
        case Id =>
          g.id()
        case Identity =>
          g.identity()
        case InE(edgeLabels @ _*) =>
          g.inE(edgeLabels: _*)
        case InV =>
          g.inV()
        case Inject(injections @ _*) =>
          g.inject(injections.map(generateValue): _*)
        case Is(predicate) =>
          g.is(generatePredicate(predicate))
        case Key =>
          g.key()
        case Label =>
          g.label()
        case Limit(limit) =>
          g.limit(limit)
        case Local(traversal) =>
          g.local(generateSteps(traversal))
        case Loops =>
          g.loops()
        case MapF(function) =>
          g.map(function)
        case MapT(traversal) =>
          g.map(generateSteps(traversal))
        case Math(expression) =>
          g.math(expression)
        case Max =>
          g.max()
        case Mean =>
          g.mean()
        case Min =>
          g.min()
        case Not(notTraversal) =>
          g.not(generateSteps(notTraversal))
        case Optional(optionalTraversal) =>
          g.optional(generateSteps(optionalTraversal))
        case Or(orTraversals @ _*) =>
          g.or(orTraversals.map(generateSteps): _*)
        case Order =>
          g.order()
        case OtherV =>
          g.otherV()
        case OutE(edgeLabels @ _*) =>
          g.outE(edgeLabels: _*)
        case OutV =>
          g.outV()
        case Path =>
          g.path()
        case Properties(propertyKeys @ _*) =>
          g.properties(propertyKeys: _*)
        case PropertyV(key, value) =>
          g.property(key, generateValue(value))
        case PropertyT(key, traversal) =>
          g.property(key, generateSteps(traversal))
        case Project(keys @ _*) =>
          g.project(keys: _*)
        case Range(scope: Scope, low: Long, high: Long) =>
          g.range(scope, low, high)
        case Repeat(repeatTraversal) =>
          g.repeat(generateSteps(repeatTraversal))
        case SelectK(selectKeys @ _*) =>
          g.select(selectKeys: _*)
        case SelectC(column) =>
          g.select(column)
        case SideEffect(sideEffectTraversal) =>
          g.sideEffect(generateSteps(sideEffectTraversal))
        case Skip(skip) =>
          g.skip(skip)
        case Sum =>
          g.sum()
        case Times(maxLoops) =>
          g.times(maxLoops)
        case To(toStepLabel) =>
          g.to(toStepLabel)
        case Unfold =>
          g.unfold()
        case Union(unionTraversals @ _*) =>
          g.union(unionTraversals.map(generateSteps): _*)
        case Until(untilTraversal) =>
          g.until(generateSteps(untilTraversal))
        case Value =>
          g.value()
        case ValueMap =>
          g.valueMap()
        case ValueMap(includeTokens) =>
          g.valueMap(includeTokens)
        case Values(propertyKeys @ _*) =>
          g.values(propertyKeys: _*)
        case WhereT(whereTraversal) =>
          g.where(generateSteps(whereTraversal))
        case WhereP(predicate) =>
          g.where(generatePredicate(predicate))
      }
    }
    g
  }

  private def generateSteps(ir: Seq[GremlinStep]): GremlinSteps[T, P] = {
    generateSteps(ir, g.start())
  }

  def generatePredicate(predicate: GremlinPredicate): P = {
    predicate match {
      case Eq(value)              => p.isEq(generateValue(value))
      case Gt(value)              => p.gt(generateValue(value))
      case Gte(value)             => p.gte(generateValue(value))
      case Lt(value)              => p.lt(generateValue(value))
      case Lte(value)             => p.lte(generateValue(value))
      case Neq(value)             => p.neq(generateValue(value))
      case Between(first, second) => p.between(generateValue(first), generateValue(second))
      case Within(values @ _*)    => p.within(values.map(generateValue): _*)
      case Without(values @ _*)   => p.without(values.map(generateValue): _*)
      case StartsWith(value)      => p.startsWith(generateValue(value))
      case EndsWith(value)        => p.endsWith(generateValue(value))
      case Contains(value)        => p.contains(generateValue(value))
    }
  }

  def generateValue(value: Any): Object = {
    value match {
      case GremlinBinding(name, bValue) => b.bind(name, bValue)
      case _                            => value.asInstanceOf[AnyRef]
    }
  }
}
