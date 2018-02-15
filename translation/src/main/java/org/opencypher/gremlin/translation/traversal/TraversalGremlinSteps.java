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
package org.opencypher.gremlin.translation.traversal;


import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.AddVertexStartStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opencypher.gremlin.translation.AliasHistory;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.traversal.CustomFunction;

@SuppressWarnings("unchecked")
public class TraversalGremlinSteps implements GremlinSteps<GraphTraversal, P> {

    private final GraphTraversal g;
    private TraversalGremlinSteps parent;
    private AliasHistory aliasHistory;

    public TraversalGremlinSteps(GraphTraversal g) {
        this(g, null);
    }

    private TraversalGremlinSteps(GraphTraversal g, TraversalGremlinSteps parent) {
        this.g = g;
        this.parent = parent;
        this.aliasHistory = parent != null ? parent.aliasHistory.copy() : new AliasHistory();
    }

    @Override
    public TraversalGremlinSteps copy() {
        return new TraversalGremlinSteps(g.asAdmin().clone());
    }

    @Override
    public GremlinSteps<GraphTraversal, P> mutate(Consumer<GremlinSteps<GraphTraversal, P>> mutator) {
        mutator.accept(this);
        return this;
    }

    @Override
    public GraphTraversal current() {
        return g;
    }

    @Override
    public String alias(String label) {
        return aliasHistory.current(label);
    }

    private boolean isStarted() {
        return g.asAdmin().getSteps().size() > 0;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> start() {
        GraphTraversal g = __.start();
        return new TraversalGremlinSteps(g, this);
    }

    @Override
    public GremlinSteps<GraphTraversal, P> V() {
        if (isStarted() || parent != null) {
            g.V();
        } else {
            // Workaround for constructing `GraphStep` with `isStart == true`
            g.asAdmin().getBytecode().addStep(GraphTraversal.Symbols.V);
            g.asAdmin().addStep(new GraphStep<>(g.asAdmin(), Vertex.class, true));
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> addE(String edgeLabel) {
        g.addE(edgeLabel);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> addV() {
        if (isStarted()) {
            g.addV();
        } else {
            // Workaround for constructing `GraphStep` with `isStart == true`
            g.asAdmin().getBytecode().addStep(GraphTraversal.Symbols.addV);
            g.asAdmin().addStep(new AddVertexStartStep(g.asAdmin(), (String) null));
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> addV(String vertexLabel) {
        if (isStarted()) {
            g.addV(vertexLabel);
        } else {
            // Workaround for constructing `GraphStep` with `isStart == true`
            g.asAdmin().getBytecode().addStep(GraphTraversal.Symbols.addV, vertexLabel);
            g.asAdmin().addStep(new AddVertexStartStep(g.asAdmin(), vertexLabel));
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> aggregate(String label) {
        g.aggregate(label);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> and(GremlinSteps<GraphTraversal, P>... ands) {
        g.and(traversals(ands));
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> as(String label) {
        g.as(aliasHistory.next(label));
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> barrier() {
        g.barrier();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> bothE(String... edgeLabels) {
        g.bothE(edgeLabels);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> by(GremlinSteps<GraphTraversal, P> traversal, Order order) {
        g.by(traversal.current(), order);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> by(GremlinSteps<GraphTraversal, P> traversal) {
        g.by(traversal.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> cap(String label) {
        g.cap(label);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> choose(final GremlinSteps<GraphTraversal, P> traversalPredicate,
                                                  GremlinSteps<GraphTraversal, P> trueChoice,
                                                  GremlinSteps<GraphTraversal, P> falseChoice) {
        g.choose(traversalPredicate.current(), trueChoice.current(), falseChoice.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> choose(P predicate, GremlinSteps<GraphTraversal, P> trueChoice, GremlinSteps<GraphTraversal, P> falseChoice) {
        g.choose(predicate, trueChoice.current(), falseChoice.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> choose(P predicate, GremlinSteps<GraphTraversal, P> trueChoice) {
        g.choose(predicate, trueChoice.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> coalesce(GremlinSteps<GraphTraversal, P>... traversals) {
        g.coalesce(traversals(traversals));
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> constant(Object e) {
        g.constant(e);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> count() {
        g.count();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> count(Scope scope) {
        g.count(Scope.local);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> dedup() {
        g.dedup();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> drop() {
        g.drop();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> emit() {
        g.emit();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> fold() {
        g.fold();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> from(String stepLabel) {
        g.from(stepLabel);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> group() {
        g.group();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> has(String propertyKey) {
        g.has(propertyKey);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> hasKey(String... keys) {
        if (keys.length >= 1) {
            g.hasKey(keys[0], arraySlice(keys, 1));
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> hasLabel(String... labels) {
        if (labels.length >= 1) {
            g.hasLabel(labels[0], arraySlice(labels, 1));
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> hasNot(String propertyKey) {
        g.hasNot(propertyKey);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> id() {
        g.id();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> identity() {
        g.identity();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> inE(String... edgeLabels) {
        g.inE(edgeLabels);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> inV() {
        g.inV();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> inject(Object... injections) {
        g.inject(injections);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> is(P predicate) {
        g.is(predicate);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> key() {
        g.key();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> label() {
        g.label();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> limit(long limit) {
        g.limit(limit);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> loops() {
        g.loops();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> map(CustomFunction function) {
        g.map(function);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> max() {
        g.max();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> mean() {
        g.mean();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> min() {
        g.min();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> not(GremlinSteps<GraphTraversal, P> rhs) {
        g.not(rhs.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> or(GremlinSteps<GraphTraversal, P>... ors) {
        g.or(traversals(ors));
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> order() {
        g.order();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> otherV() {
        g.otherV();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> outE(String... edgeLabels) {
        g.outE(edgeLabels);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> outV() {
        g.outV();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> path() {
        g.path();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> properties(String... propertyKeys) {
        g.properties(propertyKeys);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> property(String key, Object value) {
        g.property(key, value);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> property(String key, GremlinSteps<GraphTraversal, P> builder) {
        return property(key, builder.current());
    }

    @Override
    public GremlinSteps<GraphTraversal, P> project(String... keys) {
        if (keys.length < 1) {
            throw new IllegalArgumentException("`project()` step requires keys");
        } else if (keys.length == 1) {
            g.project(keys[0]);
        } else {
            String[] hack = new String[keys.length - 1];
            System.arraycopy(keys, 1, hack, 0, keys.length - 1);
            g.project(keys[0], hack);
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> range(long low, long high) {
        g.range(low, high);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> repeat(GremlinSteps<GraphTraversal, P> gremlinSteps) {
        g.repeat(gremlinSteps.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> select(String... stepLabels) {
        String[] aliases = Stream.of(stepLabels)
            .map(aliasHistory::current)
            .toArray(String[]::new);
        return selectLabels(aliases);
    }

    @Override
    public GremlinSteps<GraphTraversal, P> selectLabels(String... stepLabels) {
        if (stepLabels.length >= 2) {
            g.select(stepLabels[0], stepLabels[1], arraySlice(stepLabels, 2));
        } else if (stepLabels.length == 1) {
            g.select(stepLabels[0]);
        } else {
            throw new IllegalArgumentException("Select step should have arguments");
        }
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> select(Column column) {
        g.select(column);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> sideEffect(GremlinSteps<GraphTraversal, P> gremlinSteps) {
        g.sideEffect(gremlinSteps.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> skip(long skip) {
        g.skip(skip);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> sum() {
        g.sum();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> times(Integer maxLoops) {
        g.times(maxLoops);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> to(String stepLabel) {
        g.to(stepLabel);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> unfold() {
        g.unfold();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> union(GremlinSteps<GraphTraversal, P>... gremlinSteps) {
        g.union(traversals(gremlinSteps));
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> until(GremlinSteps<GraphTraversal, P> gremlinSteps) {
        g.until(gremlinSteps.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> valueMap() {
        g.valueMap();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> value() {
        g.value();
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> values(String... propertyKeys) {
        g.values(propertyKeys);
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> where(GremlinSteps<GraphTraversal, P> gremlinSteps) {
        g.where(gremlinSteps.current());
        return this;
    }

    @Override
    public GremlinSteps<GraphTraversal, P> where(P predicate) {
        g.where(predicate);
        return this;
    }

    private static String[] arraySlice(String[] array, int start) {
        String[] dest = new String[array.length - start];
        System.arraycopy(array, start, dest, 0, array.length - start);
        return dest;
    }

    private GraphTraversal[] traversals(GremlinSteps<GraphTraversal, P>[] gremlinSteps) {
        return Stream.of(gremlinSteps)
            .map(GremlinSteps::current)
            .toArray(GraphTraversal[]::new);
    }

    @Override
    public String toString() {
        return g.asAdmin().getBytecode().toString();
    }
}
