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

import java.util

import org.apache.tinkerpop.gremlin.process.traversal.Scope
import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.translation.ir.verify._
import org.opencypher.gremlin.translation.translator.TranslatorFeature._
import org.opencypher.gremlin.translation.translator.{Translator, TranslatorFeature}

import scala.collection.JavaConverters._

/**
  * Translation writer that produces to-Gremlin translation
  * based on intermediate representation of a query.
  */
object TranslationWriter {

  /**
    * Produces query translation.
    *
    * @param ir         intermediate representation of the translation
    * @param translator instance of [[Translator]]
    * @param parameters Cypher query parameters
    * @tparam T translation target type
    * @tparam P predicate target type
    * @return to-Gremlin translation
    */
  def write[T, P](ir: Seq[GremlinStep], translator: Translator[T, P], parameters: util.Map[String, Any]): T = {
    write(ir, translator, parameters.asScala.toMap)
  }

  private val postConditions: Map[TranslatorFeature, GremlinPostCondition] = Map(
    CYPHER_EXTENSIONS -> NoCustomFunctions,
    MULTIPLE_LABELS -> NoMultipleLabels
  )

  def write[T, P](ir: Seq[GremlinStep], translator: Translator[T, P], parameters: Map[String, Any]): T = {
    for ((feature, postCondition) <- postConditions if !translator.isEnabled(feature);
         msg <- postCondition(ir)) throw new SyntaxException(msg)

    val generator = new TranslationWriter(translator, parameters)
    generator.writeSteps(ir, translator.steps())
    translator.translate()
  }
}

sealed class TranslationWriter[T, P] private (translator: Translator[T, P], parameters: Map[String, Any]) {
  private val g = translator.steps()
  private val p = translator.predicates()
  private val b = translator.bindings()

  private def writeSteps(ir: Seq[GremlinStep], g: GremlinSteps[T, P]): GremlinSteps[T, P] = {
    for (step <- ir) {
      step match {
        case Vertex =>
          g.V()
        case Edge =>
          g.E()
        case AddE(edgeLabel) =>
          g.addE(edgeLabel)
        case AddV =>
          g.addV()
        case AddV(vertexLabel) =>
          g.addV(vertexLabel)
        case Aggregate(sideEffectKey) =>
          g.aggregate(sideEffectKey)
        case And(andTraversals @ _*) =>
          g.and(andTraversals.map(writeLocalSteps): _*)
        case As(stepLabel) =>
          g.as(stepLabel)
        case Barrier =>
          g.barrier()
        case BothE(edgeLabels @ _*) =>
          g.bothE(edgeLabels: _*)
        case By(traversal, order) =>
          order
            .map(g.by(writeLocalSteps(traversal), _))
            .getOrElse(g.by(writeLocalSteps(traversal)))
        case Cap(sideEffectKey) =>
          g.cap(sideEffectKey)
        case ChooseT(choiceTraversal, None, None) =>
          g.choose(writeLocalSteps(choiceTraversal))
        case c @ ChooseT(_, None, Some(_)) =>
          throw new UnsupportedOperationException(s"Unsupported $c")
        case c @ ChooseT(_, Some(_), None) =>
          throw new UnsupportedOperationException(s"Unsupported $c")
        case ChooseT(traversalPredicate, Some(trueChoice), Some(falseChoice)) =>
          g.choose(writeLocalSteps(traversalPredicate), writeLocalSteps(trueChoice), writeLocalSteps(falseChoice))
        case ChooseP(predicate, trueChoice, None) =>
          g.choose(writePredicate(predicate), writeLocalSteps(trueChoice))
        case ChooseP(predicate, trueChoice, Some(falseChoice)) =>
          g.choose(writePredicate(predicate), writeLocalSteps(trueChoice), writeLocalSteps(falseChoice))
        case Coalesce(coalesceTraversals @ _*) =>
          g.coalesce(coalesceTraversals.map(writeLocalSteps): _*)
        case Constant(e) =>
          g.constant(writeValue(e))
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
        case FlatMapT(traversal) =>
          g.flatMap(writeLocalSteps(traversal))
        case Fold =>
          g.fold()
        case From(fromStepLabel) =>
          g.from(fromStepLabel)
        case Group =>
          g.group()
        case Has(propertyKey) =>
          g.has(propertyKey)
        case HasP(propertyKey, predicate) =>
          g.has(propertyKey, writePredicate(predicate))
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
          g.inject(injections.map(writeValue): _*)
        case Is(predicate) =>
          g.is(writePredicate(predicate))
        case Key =>
          g.key()
        case Label =>
          g.label()
        case Limit(limit) =>
          g.limit(limit)
        case LimitS(scope, limit) =>
          g.limit(scope, limit)
        case Local(traversal) =>
          g.local(writeLocalSteps(traversal))
        case Loops =>
          g.loops()
        case MapF(function) =>
          g.map(function)
        case MapT(traversal) =>
          g.map(writeLocalSteps(traversal))
        case Math(expression) =>
          g.math(expression)
        case Max =>
          g.max()
        case Mean =>
          g.mean()
        case Min =>
          g.min()
        case Not(notTraversal) =>
          g.not(writeLocalSteps(notTraversal))
        case OptionT(pickToken, optionalTraversal) =>
          g.option(pickToken, writeLocalSteps(optionalTraversal))
        case Optional(optionalTraversal) =>
          g.optional(writeLocalSteps(optionalTraversal))
        case Or(orTraversals @ _*) =>
          g.or(orTraversals.map(writeLocalSteps): _*)
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
          g.property(key, writeValue(value))
        case PropertyVC(cardinality, key, value) =>
          g.property(cardinality, key, writeValue(value))
        case PropertyT(key, traversal) =>
          g.property(key, writeLocalSteps(traversal))
        case PropertyTC(cardinality, key, traversal) =>
          g.property(cardinality, key, writeLocalSteps(traversal))
        case Project(keys @ _*) =>
          g.project(keys: _*)
        case Range(scope: Scope, low: Long, high: Long) =>
          g.range(scope, low, high)
        case Repeat(repeatTraversal) =>
          g.repeat(writeLocalSteps(repeatTraversal))
        case SelectK(selectKeys @ _*) =>
          g.select(selectKeys: _*)
        case SelectC(column) =>
          g.select(column)
        case SideEffect(sideEffectTraversal) =>
          g.sideEffect(writeLocalSteps(sideEffectTraversal))
        case SimplePath =>
          g.simplePath()
        case Skip(skip) =>
          g.skip(skip)
        case Sum =>
          g.sum()
        case Tail(scope, limit) =>
          g.tail(scope, limit)
        case Times(maxLoops) =>
          g.times(maxLoops)
        case To(toStepLabel) =>
          g.to(toStepLabel)
        case Unfold =>
          g.unfold()
        case Union(unionTraversals @ _*) =>
          g.union(unionTraversals.map(writeLocalSteps): _*)
        case Until(untilTraversal) =>
          g.until(writeLocalSteps(untilTraversal))
        case Value =>
          g.value()
        case ValueMap =>
          g.valueMap()
        case ValueMap(includeTokens) =>
          g.valueMap(includeTokens)
        case Values(propertyKeys @ _*) =>
          g.values(propertyKeys: _*)
        case WhereT(whereTraversal) =>
          g.where(writeLocalSteps(whereTraversal))
        case WhereP(predicate) =>
          g.where(writePredicate(predicate))
      }
    }
    g
  }

  private def writeLocalSteps(ir: Seq[GremlinStep]): GremlinSteps[T, P] = {
    writeSteps(ir, g.start())
  }

  def writePredicate(predicate: GremlinPredicate): P = {
    predicate match {
      case Eq(value)              => p.isEq(writeValue(value))
      case Gt(value)              => p.gt(writeValue(value))
      case Gte(value)             => p.gte(writeValue(value))
      case Lt(value)              => p.lt(writeValue(value))
      case Lte(value)             => p.lte(writeValue(value))
      case Neq(value)             => p.neq(writeValue(value))
      case Between(first, second) => p.between(writeValue(first), writeValue(second))
      case Within(values @ _*)    => p.within(values.map(writeValue): _*)
      case Without(values @ _*)   => p.without(values.map(writeValue): _*)
      case StartsWith(value)      => p.startsWith(writeValue(value))
      case EndsWith(value)        => p.endsWith(writeValue(value))
      case Contains(value)        => p.contains(writeValue(value))
      case IsNode()               => p.isNode
      case IsRelationship()       => p.isRelationship
    }
  }

  def writeValue(value: Any): Object = {
    value match {
      case GremlinBinding(name) => b.bind(name, parameters.get(name).orNull)
      case _                    => value.asInstanceOf[AnyRef]
    }
  }
}
