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
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Column;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

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
 * @see TranslatorFactory
 */
public interface TranslationBuilder<T, P> {
    T current();

    String alias(String label);

    TranslationBuilder<T, P> start();

    TranslationBuilder<T, P> copy();

    TranslationBuilder<T, P> mutate(Consumer<TranslationBuilder<T, P>> mutator);

    TranslationBuilder<T, P> V();

    TranslationBuilder<T, P> addE(String edgeLabel);

    TranslationBuilder<T, P> addV();

    TranslationBuilder<T, P> addV(String vertexLabel);

    TranslationBuilder<T, P> aggregate(String label);

    @SuppressWarnings("unchecked")
    TranslationBuilder<T, P> and(TranslationBuilder<T, P>... ands);

    TranslationBuilder<T, P> as(String label);

    TranslationBuilder<T, P> barrier();

    TranslationBuilder<T, P> bothE(String... edgeLabels);

    TranslationBuilder<T, P> by(TranslationBuilder<T, P> traversal);

    TranslationBuilder<T, P> by(TranslationBuilder<T, P> traversal, Order order);

    TranslationBuilder<T, P> choose(TranslationBuilder<T, P> traversalPredicate, TranslationBuilder<T, P> trueChoice, TranslationBuilder<T, P> falseChoice);

    TranslationBuilder<T, P> choose(P predicate, TranslationBuilder<T, P> trueChoice, TranslationBuilder<T, P> falseChoice);

    TranslationBuilder<T, P> choose(P predicate, TranslationBuilder<T, P> trueChoice);

    @SuppressWarnings("unchecked")
    TranslationBuilder<T, P> coalesce(TranslationBuilder<T, P>... traversals);

    TranslationBuilder<T, P> constant(Object e);

    TranslationBuilder<T, P> count();

    TranslationBuilder<T, P> count(Scope scope);

    TranslationBuilder<T, P> dedup();

    TranslationBuilder<T, P> drop();

    TranslationBuilder<T, P> emit();

    TranslationBuilder<T, P> fold();

    TranslationBuilder<T, P> from(String stepLabel);

    TranslationBuilder<T, P> group();

    TranslationBuilder<T, P> has(String propertyKey);

    TranslationBuilder<T, P> hasKey(String... keys);

    TranslationBuilder<T, P> hasLabel(String... labels);

    TranslationBuilder<T, P> hasNot(String propertyKey);

    TranslationBuilder<T, P> inE(String... edgeLabels);

    TranslationBuilder<T, P> inV();

    TranslationBuilder<T, P> inject(Object... injections);

    TranslationBuilder<T, P> is(P predicate);

    TranslationBuilder<T, P> label();

    TranslationBuilder<T, P> limit(long limit);

    TranslationBuilder<T, P> map(String functionName, Function<Traverser, Object> function);

    TranslationBuilder<T, P> max();

    TranslationBuilder<T, P> mean();

    TranslationBuilder<T, P> min();

    TranslationBuilder<T, P> not(TranslationBuilder<T, P> rhs);

    @SuppressWarnings("unchecked")
    TranslationBuilder<T, P> or(TranslationBuilder<T, P>... ors);

    TranslationBuilder<T, P> order();

    TranslationBuilder<T, P> otherV();

    TranslationBuilder<T, P> outE(String... edgeLabels);

    TranslationBuilder<T, P> outV();

    TranslationBuilder<T, P> path();

    TranslationBuilder<T, P> properties(String... propertyKeys);

    TranslationBuilder<T, P> property(String key, Object value);

    TranslationBuilder<T, P> project(String... keys);

    TranslationBuilder<T, P> propertyList(String key, Collection values);

    TranslationBuilder<T, P> range(long low, long high);

    TranslationBuilder<T, P> repeat(TranslationBuilder<T, P> translationBuilder);

    TranslationBuilder<T, P> select(String... stepLabels);

    TranslationBuilder<T, P> selectLabels(String... stepLabels);

    TranslationBuilder<T, P> select(Column column);

    TranslationBuilder<T, P> sideEffect(TranslationBuilder<T, P> translationBuilder);

    TranslationBuilder<T, P> skip(long skip);

    TranslationBuilder<T, P> sum();

    TranslationBuilder<T, P> times(Integer maxLoops);

    TranslationBuilder<T, P> to(String stepLabel);

    TranslationBuilder<T, P> unfold();

    @SuppressWarnings("unchecked")
    TranslationBuilder<T, P> union(TranslationBuilder<T, P>... translationBuilders);

    TranslationBuilder<T, P> until(TranslationBuilder<T, P> translationBuilder);

    TranslationBuilder<T, P> value();

    TranslationBuilder<T, P> valueMap();

    TranslationBuilder<T, P> values(String... propertyKeys);

    TranslationBuilder<T, P> where(TranslationBuilder<T, P> translationBuilder);

    TranslationBuilder<T, P> where(P predicate);
}
