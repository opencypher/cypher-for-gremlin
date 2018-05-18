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
package org.opencypher.gremlin.translation.bytecode;

import static org.opencypher.gremlin.translation.groovy.StringTranslationUtils.apply;

import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode.Binding;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal.Symbols;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.apache.tinkerpop.gremlin.util.function.Lambda;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.traversal.CustomFunction;

@SuppressWarnings("unchecked")
public class BytecodeGremlinSteps implements GremlinSteps<Bytecode, P> {

    private final Bytecode bytecode;

    public BytecodeGremlinSteps() {
        this.bytecode = new Bytecode();
    }

    @Override
    public Bytecode current() {
        return bytecode.clone();
    }

    @Override
    public GremlinSteps<Bytecode, P> start() {
        return new BytecodeGremlinSteps();
    }

    @Override
    public GremlinSteps<Bytecode, P> V() {
        bytecode.addStep(Symbols.V);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> addE(String edgeLabel) {
        bytecode.addStep(Symbols.addE, edgeLabel);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> addV() {
        bytecode.addStep(Symbols.addV);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> addV(String vertexLabel) {
        bytecode.addStep(Symbols.addV, vertexLabel);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> aggregate(String sideEffectKey) {
        bytecode.addStep(Symbols.aggregate, sideEffectKey);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> and(GremlinSteps<Bytecode, P>... andTraversals) {
        bytecode.addStep(Symbols.and, (Object[]) traversals(andTraversals));
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> as(String stepLabel) {
        bytecode.addStep(Symbols.as, stepLabel);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> barrier() {
        bytecode.addStep(Symbols.barrier);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> bothE(String... edgeLabels) {
        bytecode.addStep(Symbols.bothE, (Object[]) edgeLabels);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> by(GremlinSteps<Bytecode, P> traversal) {
        bytecode.addStep(Symbols.by, traversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> by(GremlinSteps<Bytecode, P> traversal, Order order) {
        bytecode.addStep(Symbols.by, traversal.current(), order);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> cap(String sideEffectKey) {
        bytecode.addStep(Symbols.cap, sideEffectKey);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> choose(GremlinSteps<Bytecode, P> traversalPredicate,
                                            GremlinSteps<Bytecode, P> trueChoice,
                                            GremlinSteps<Bytecode, P> falseChoice) {
        bytecode.addStep(Symbols.choose, traversalPredicate.current(), trueChoice.current(), falseChoice.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> choose(P predicate,
                                            GremlinSteps<Bytecode, P> trueChoice,
                                            GremlinSteps<Bytecode, P> falseChoice) {
        bytecode.addStep(Symbols.choose, predicate, trueChoice.current(), falseChoice.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> choose(P predicate, GremlinSteps<Bytecode, P> trueChoice) {
        bytecode.addStep(Symbols.choose, predicate, trueChoice.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> coalesce(GremlinSteps<Bytecode, P>... coalesceTraversals) {
        bytecode.addStep(Symbols.coalesce, (Object[]) traversals(coalesceTraversals));
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> constant(Object e) {
        bytecode.addStep(Symbols.constant, e);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> count() {
        bytecode.addStep(Symbols.count);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> count(Scope scope) {
        bytecode.addStep(Symbols.count, scope);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> dedup() {
        bytecode.addStep(Symbols.dedup);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> drop() {
        bytecode.addStep(Symbols.drop);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> emit() {
        bytecode.addStep(Symbols.emit);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> fold() {
        bytecode.addStep(Symbols.fold);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> from(String fromStepLabel) {
        bytecode.addStep(Symbols.from, fromStepLabel);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> group() {
        bytecode.addStep(Symbols.group);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> has(String propertyKey) {
        bytecode.addStep(Symbols.has, propertyKey);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> has(String propertyKey, P predicate) {
        bytecode.addStep(Symbols.has, propertyKey, predicate);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> hasKey(String... labels) {
        bytecode.addStep(Symbols.hasKey, (Object[]) labels);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> hasLabel(String... labels) {
        bytecode.addStep(Symbols.hasLabel, (Object[]) labels);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> hasNot(String propertyKey) {
        bytecode.addStep(Symbols.hasNot, propertyKey);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> id() {
        bytecode.addStep(Symbols.id);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> identity() {
        bytecode.addStep(Symbols.identity);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> inE(String... edgeLabels) {
        bytecode.addStep(Symbols.inE, (Object[]) edgeLabels);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> inV() {
        bytecode.addStep(Symbols.inV);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> inject(Object... injections) {
        bytecode.addStep(Symbols.inject, (Object[]) injections);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> is(P predicate) {
        bytecode.addStep(Symbols.is, predicate);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> key() {
        bytecode.addStep(Symbols.key);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> label() {
        bytecode.addStep(Symbols.label);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> limit(long limit) {
        bytecode.addStep(Symbols.limit, limit);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> local(GremlinSteps<Bytecode, P> localTraversal) {
        bytecode.addStep(Symbols.local, localTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> loops() {
        bytecode.addStep(Symbols.loops);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> map(CustomFunction function) {
        Object[] args = Stream.of(function.getArgs())
            .map(arg -> (arg instanceof Binding) ? ((Binding) arg).value() : arg)
            .toArray();
        String lambdaSource = apply(function.getName(), args) + ".apply(it)";
        Function lambda = Lambda.function(lambdaSource, "gremlin-groovy");
        bytecode.addStep(Symbols.map, lambda);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> map(GremlinSteps<Bytecode, P> traversal) {
        bytecode.addStep(Symbols.map, traversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> math(String expression) {
        bytecode.addStep(Symbols.math, expression);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> max() {
        bytecode.addStep(Symbols.max);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> mean() {
        bytecode.addStep(Symbols.mean);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> min() {
        bytecode.addStep(Symbols.min);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> not(GremlinSteps<Bytecode, P> notTraversal) {
        bytecode.addStep(Symbols.not, notTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> optional(GremlinSteps<Bytecode, P> optionalTraversal) {
        bytecode.addStep(Symbols.optional, optionalTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> or(GremlinSteps<Bytecode, P>... orTraversals) {
        bytecode.addStep(Symbols.or, (Object[]) traversals(orTraversals));
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> order() {
        bytecode.addStep(Symbols.order);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> otherV() {
        bytecode.addStep(Symbols.otherV);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> outE(String... edgeLabels) {
        bytecode.addStep(Symbols.outE, (Object[]) edgeLabels);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> outV() {
        bytecode.addStep(Symbols.outV);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> path() {
        bytecode.addStep(Symbols.path);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> properties(String... propertyKeys) {
        bytecode.addStep(Symbols.properties, (Object[]) propertyKeys);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> property(String key, Object value) {
        bytecode.addStep(Symbols.property, key, value);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> property(String key, GremlinSteps<Bytecode, P> traversal) {
        bytecode.addStep(Symbols.property, key, traversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> project(String... keys) {
        bytecode.addStep(Symbols.project, (Object[]) keys);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> range(Scope scope, long low, long high) {
        bytecode.addStep(Symbols.range, scope, low, high);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> repeat(GremlinSteps<Bytecode, P> repeatTraversal) {
        bytecode.addStep(Symbols.repeat, repeatTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> select(String... selectKeys) {
        bytecode.addStep(Symbols.select, (Object[]) selectKeys);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> select(Column column) {
        bytecode.addStep(Symbols.select, column);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> sideEffect(GremlinSteps<Bytecode, P> sideEffectTraversal) {
        bytecode.addStep(Symbols.sideEffect, sideEffectTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> skip(long skip) {
        bytecode.addStep(Symbols.skip, skip);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> sum() {
        bytecode.addStep(Symbols.sum);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> times(int maxLoops) {
        bytecode.addStep(Symbols.times, maxLoops);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> to(String toStepLabel) {
        bytecode.addStep(Symbols.to, toStepLabel);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> unfold() {
        bytecode.addStep(Symbols.unfold);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> union(GremlinSteps<Bytecode, P>... unionTraversals) {
        bytecode.addStep(Symbols.union, (Object[]) traversals(unionTraversals));
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> until(GremlinSteps<Bytecode, P> untilTraversal) {
        bytecode.addStep(Symbols.until, untilTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> value() {
        bytecode.addStep(Symbols.value);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> valueMap() {
        bytecode.addStep(Symbols.valueMap);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> valueMap(boolean includeTokens) {
        bytecode.addStep(Symbols.valueMap, includeTokens);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> values(String... propertyKeys) {
        bytecode.addStep(Symbols.values, (Object[]) propertyKeys);
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> where(GremlinSteps<Bytecode, P> whereTraversal) {
        bytecode.addStep(Symbols.where, whereTraversal.current());
        return this;
    }

    @Override
    public GremlinSteps<Bytecode, P> where(P predicate) {
        bytecode.addStep(Symbols.where, predicate);
        return this;
    }

    private static Bytecode[] traversals(GremlinSteps<Bytecode, P>[] gremlinSteps) {
        return Stream.of(gremlinSteps)
            .map(GremlinSteps::current)
            .toArray(Bytecode[]::new);
    }

    @Override
    public String toString() {
        return bytecode.toString();
    }
}
