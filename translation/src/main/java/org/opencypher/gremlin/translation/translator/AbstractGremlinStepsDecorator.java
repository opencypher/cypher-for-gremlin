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
package org.opencypher.gremlin.translation.translator;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.opencypher.gremlin.translation.GremlinSteps;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

abstract class AbstractGremlinStepsDecorator<T, P> implements GremlinSteps<T, P> {

    protected AbstractGremlinStepsDecorator() {
    }

    protected abstract GremlinSteps<T, P> delegate();

    @Override
    public T current() {
        return delegate().current();
    }

    @Override
    public String alias(String label) {
        return delegate().alias(label);
    }

    @Override
    public GremlinSteps<T, P> start() {
        return delegate().start();
    }

    @Override
    public GremlinSteps<T, P> copy() {
        return delegate().copy();
    }

    @Override
    public GremlinSteps<T, P> mutate(Consumer<GremlinSteps<T, P>> mutator) {
        return delegate().mutate(mutator);
    }

    @Override
    public GremlinSteps<T, P> V() {
        return delegate().V();
    }

    @Override
    public GremlinSteps<T, P> addE(String edgeLabel) {
        return delegate().addE(edgeLabel);
    }

    @Override
    public GremlinSteps<T, P> addV() {
        return delegate().addV();
    }

    @Override
    public GremlinSteps<T, P> addV(String vertexLabel) {
        return delegate().addV(vertexLabel);
    }

    @Override
    public GremlinSteps<T, P> aggregate(String label) {
        return delegate().aggregate(label);
    }

    @Override
    public GremlinSteps<T, P> and(GremlinSteps<T, P>... ands) {
        return delegate().and(ands);
    }

    @Override
    public GremlinSteps<T, P> as(String label) {
        return delegate().as(label);
    }

    @Override
    public GremlinSteps<T, P> barrier() {
        return delegate().barrier();
    }

    @Override
    public GremlinSteps<T, P> bothE(String... edgeLabels) {
        return delegate().bothE(edgeLabels);
    }

    @Override
    public GremlinSteps<T, P> by(GremlinSteps<T, P> traversal) {
        return delegate().by(traversal);
    }

    @Override
    public GremlinSteps<T, P> by(GremlinSteps<T, P> traversal, Order order) {
        return delegate().by(traversal, order);
    }

    @Override
    public GremlinSteps<T, P> choose(GremlinSteps<T, P> traversalPredicate, GremlinSteps<T, P> trueChoice, GremlinSteps<T, P> falseChoice) {
        return delegate().choose(traversalPredicate, trueChoice, falseChoice);
    }

    @Override
    public GremlinSteps<T, P> choose(P predicate, GremlinSteps<T, P> trueChoice, GremlinSteps<T, P> falseChoice) {
        return delegate().choose(predicate, trueChoice, falseChoice);
    }

    @Override
    public GremlinSteps<T, P> choose(P predicate, GremlinSteps<T, P> trueChoice) {
        return delegate().choose(predicate, trueChoice);
    }

    @Override
    public GremlinSteps<T, P> coalesce(GremlinSteps<T, P>... traversals) {
        return delegate().coalesce(traversals);
    }

    @Override
    public GremlinSteps<T, P> constant(Object e) {
        return delegate().constant(e);
    }

    @Override
    public GremlinSteps<T, P> count() {
        return delegate().count();
    }

    @Override
    public GremlinSteps<T, P> count(Scope scope) {
        return delegate().count(scope);
    }

    @Override
    public GremlinSteps<T, P> dedup() {
        return delegate().dedup();
    }

    @Override
    public GremlinSteps<T, P> drop() {
        return delegate().drop();
    }

    @Override
    public GremlinSteps<T, P> emit() {
        return delegate().emit();
    }

    @Override
    public GremlinSteps<T, P> fold() {
        return delegate().fold();
    }

    @Override
    public GremlinSteps<T, P> from(String stepLabel) {
        return delegate().from(stepLabel);
    }

    @Override
    public GremlinSteps<T, P> group() {
        return delegate().group();
    }

    @Override
    public GremlinSteps<T, P> has(String propertyKey) {
        return delegate().has(propertyKey);
    }

    @Override
    public GremlinSteps<T, P> hasKey(String... keys) {
        return delegate().hasKey(keys);
    }

    @Override
    public GremlinSteps<T, P> hasLabel(String... labels) {
        return delegate().hasLabel(labels);
    }

    @Override
    public GremlinSteps<T, P> hasNot(String propertyKey) {
        return delegate().hasNot(propertyKey);
    }

    @Override
    public GremlinSteps<T, P> id() {
        return delegate().id();
    }

    @Override
    public GremlinSteps<T, P> identity() {
        return delegate().identity();
    }

    @Override
    public GremlinSteps<T, P> inE(String... edgeLabels) {
        return delegate().inE(edgeLabels);
    }

    @Override
    public GremlinSteps<T, P> inV() {
        return delegate().inV();
    }

    @Override
    public GremlinSteps<T, P> inject(Object... injections) {
        return delegate().inject(injections);
    }

    @Override
    public GremlinSteps<T, P> is(P predicate) {
        return delegate().is(predicate);
    }

    @Override
    public GremlinSteps<T, P> key() {
        return delegate().key();
    }

    @Override
    public GremlinSteps<T, P> label() {
        return delegate().label();
    }

    @Override
    public GremlinSteps<T, P> limit(long limit) {
        return delegate().limit(limit);
    }

    @Override
    public GremlinSteps<T, P> map(String functionName, Function<Traverser, Object> function) {
        return delegate().map(functionName, function);
    }

    @Override
    public GremlinSteps<T, P> max() {
        return delegate().max();
    }

    @Override
    public GremlinSteps<T, P> mean() {
        return delegate().mean();
    }

    @Override
    public GremlinSteps<T, P> min() {
        return delegate().min();
    }

    @Override
    public GremlinSteps<T, P> not(GremlinSteps<T, P> rhs) {
        return delegate().not(rhs);
    }

    @Override
    public GremlinSteps<T, P> or(GremlinSteps<T, P>... ors) {
        return delegate().or(ors);
    }

    @Override
    public GremlinSteps<T, P> order() {
        return delegate().order();
    }

    @Override
    public GremlinSteps<T, P> otherV() {
        return delegate().otherV();
    }

    @Override
    public GremlinSteps<T, P> outE(String... edgeLabels) {
        return delegate().outE(edgeLabels);
    }

    @Override
    public GremlinSteps<T, P> outV() {
        return delegate().outV();
    }

    @Override
    public GremlinSteps<T, P> path() {
        return delegate().path();
    }

    @Override
    public GremlinSteps<T, P> properties(String... propertyKeys) {
        return delegate().properties(propertyKeys);
    }

    @Override
    public GremlinSteps<T, P> property(String key, Object value) {
        return delegate().property(key, value);
    }

    @Override
    public GremlinSteps<T, P> project(String... keys) {
        return delegate().project(keys);
    }

    @Override
    public GremlinSteps<T, P> propertyList(String key, Collection values) {
        return delegate().propertyList(key, values);
    }

    @Override
    public GremlinSteps<T, P> range(long low, long high) {
        return delegate().range(low, high);
    }

    @Override
    public GremlinSteps<T, P> repeat(GremlinSteps<T, P> gremlinSteps) {
        return delegate().repeat(gremlinSteps);
    }

    @Override
    public GremlinSteps<T, P> select(String... stepLabels) {
        return delegate().select(stepLabels);
    }

    @Override
    public GremlinSteps<T, P> selectLabels(String... stepLabels) {
        return delegate().selectLabels(stepLabels);
    }

    @Override
    public GremlinSteps<T, P> select(Column column) {
        return delegate().select(column);
    }

    @Override
    public GremlinSteps<T, P> sideEffect(GremlinSteps<T, P> gremlinSteps) {
        return delegate().sideEffect(gremlinSteps);
    }

    @Override
    public GremlinSteps<T, P> skip(long skip) {
        return delegate().skip(skip);
    }

    @Override
    public GremlinSteps<T, P> sum() {
        return delegate().sum();
    }

    @Override
    public GremlinSteps<T, P> times(Integer maxLoops) {
        return delegate().times(maxLoops);
    }

    @Override
    public GremlinSteps<T, P> to(String stepLabel) {
        return delegate().to(stepLabel);
    }

    @Override
    public GremlinSteps<T, P> unfold() {
        return delegate().unfold();
    }

    @Override
    public GremlinSteps<T, P> union(GremlinSteps<T, P>... gremlinSteps) {
        return delegate().union(gremlinSteps);
    }

    @Override
    public GremlinSteps<T, P> until(GremlinSteps<T, P> gremlinSteps) {
        return delegate().until(gremlinSteps);
    }

    @Override
    public GremlinSteps<T, P> value() {
        return delegate().value();
    }

    @Override
    public GremlinSteps<T, P> valueMap() {
        return delegate().valueMap();
    }

    @Override
    public GremlinSteps<T, P> values(String... propertyKeys) {
        return delegate().values(propertyKeys);
    }

    @Override
    public GremlinSteps<T, P> where(GremlinSteps<T, P> gremlinSteps) {
        return delegate().where(gremlinSteps);
    }

    @Override
    public GremlinSteps<T, P> where(P predicate) {
        return delegate().where(predicate);
    }
}
