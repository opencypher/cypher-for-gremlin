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
package org.opencypher.gremlin.translation;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.CustomFunction;

import java.util.function.Consumer;

/**
 * Gremlin {@link org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal} DSL wrapper.
 * <p>
 * Implementations define a translation target that can be built with
 * {@link org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal} steps.
 * <p>
 * Note: steps are expected to be side-effecting, thus, unsuitable for use in immutable contexts.
 *
 * @param <T> translation target type
 * @param <P> predicate target type
 * @see <a href="https://tinkerpop.apache.org/docs/current/reference/#graph-traversal-steps">Graph Traversal Steps</a>
 * @see Translator
 */
public interface GremlinSteps<T, P> {
    T current();

    GremlinSteps<T, P> start();

    GremlinSteps<T, P> copy();

    GremlinSteps<T, P> mutate(Consumer<GremlinSteps<T, P>> mutator);

    GremlinSteps<T, P> V();

    GremlinSteps<T, P> addE(String edgeLabel);

    GremlinSteps<T, P> addV();

    GremlinSteps<T, P> addV(String vertexLabel);

    GremlinSteps<T, P> aggregate(String label);

    GremlinSteps<T, P> and(GremlinSteps<T, P>... ands);

    GremlinSteps<T, P> as(String label);

    GremlinSteps<T, P> barrier();

    GremlinSteps<T, P> bothE(String... edgeLabels);

    GremlinSteps<T, P> by(GremlinSteps<T, P> traversal);

    GremlinSteps<T, P> by(GremlinSteps<T, P> traversal, Order order);

    GremlinSteps<T, P> cap(String label);

    GremlinSteps<T, P> choose(GremlinSteps<T, P> traversalPredicate, GremlinSteps<T, P> trueChoice, GremlinSteps<T, P> falseChoice);

    GremlinSteps<T, P> choose(P predicate, GremlinSteps<T, P> trueChoice, GremlinSteps<T, P> falseChoice);

    GremlinSteps<T, P> choose(P predicate, GremlinSteps<T, P> trueChoice);

    GremlinSteps<T, P> coalesce(GremlinSteps<T, P>... traversals);

    GremlinSteps<T, P> constant(Object e);

    GremlinSteps<T, P> count();

    GremlinSteps<T, P> count(Scope scope);

    GremlinSteps<T, P> dedup();

    GremlinSteps<T, P> drop();

    GremlinSteps<T, P> emit();

    GremlinSteps<T, P> fold();

    GremlinSteps<T, P> from(String stepLabel);

    GremlinSteps<T, P> group();

    GremlinSteps<T, P> has(String propertyKey);

    GremlinSteps<T, P> hasKey(String... keys);

    GremlinSteps<T, P> hasLabel(String... labels);

    GremlinSteps<T, P> hasNot(String propertyKey);

    GremlinSteps<T, P> id();

    GremlinSteps<T, P> identity();

    GremlinSteps<T, P> inE(String... edgeLabels);

    GremlinSteps<T, P> inV();

    GremlinSteps<T, P> inject(Object... injections);

    GremlinSteps<T, P> is(P predicate);

    GremlinSteps<T, P> key();

    GremlinSteps<T, P> label();

    GremlinSteps<T, P> limit(long limit);

    GremlinSteps<T, P> loops();

    GremlinSteps<T, P> map(CustomFunction function);

    GremlinSteps<T, P> max();

    GremlinSteps<T, P> mean();

    GremlinSteps<T, P> min();

    GremlinSteps<T, P> not(GremlinSteps<T, P> rhs);

    GremlinSteps<T, P> or(GremlinSteps<T, P>... ors);

    GremlinSteps<T, P> order();

    GremlinSteps<T, P> otherV();

    GremlinSteps<T, P> outE(String... edgeLabels);

    GremlinSteps<T, P> outV();

    GremlinSteps<T, P> path();

    GremlinSteps<T, P> properties(String... propertyKeys);

    GremlinSteps<T, P> property(String key, Object value);

    GremlinSteps<T, P> property(String key, GremlinSteps<T, P> builder);

    GremlinSteps<T, P> project(String... keys);

    GremlinSteps<T, P> range(long low, long high);

    GremlinSteps<T, P> repeat(GremlinSteps<T, P> gremlinSteps);

    GremlinSteps<T, P> select(String... stepLabels);

    GremlinSteps<T, P> select(Column column);

    GremlinSteps<T, P> sideEffect(GremlinSteps<T, P> gremlinSteps);

    GremlinSteps<T, P> skip(long skip);

    GremlinSteps<T, P> sum();

    GremlinSteps<T, P> times(Integer maxLoops);

    GremlinSteps<T, P> to(String stepLabel);

    GremlinSteps<T, P> unfold();

    GremlinSteps<T, P> union(GremlinSteps<T, P>... gremlinSteps);

    GremlinSteps<T, P> until(GremlinSteps<T, P> gremlinSteps);

    GremlinSteps<T, P> value();

    GremlinSteps<T, P> valueMap();

    GremlinSteps<T, P> values(String... propertyKeys);

    GremlinSteps<T, P> where(GremlinSteps<T, P> gremlinSteps);

    GremlinSteps<T, P> where(P predicate);
}
