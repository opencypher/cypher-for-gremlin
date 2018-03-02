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
package org.opencypher.gremlin.translation.groovy;

import static org.opencypher.gremlin.translation.groovy.StringTranslationUtils.apply;
import static org.opencypher.gremlin.translation.groovy.StringTranslationUtils.chain;

import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.traversal.CustomFunction;

public class GroovyGremlinSteps implements GremlinSteps<String, GroovyPredicate> {

    private final StringBuilder g;

    public GroovyGremlinSteps() {
        this("g");
    }

    protected GroovyGremlinSteps(String start) {
        g = new StringBuilder(start);
    }

    @Override
    public String toString() {
        return g.toString();
    }

    @Override
    public String current() {
        return g.toString();
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> start() {
        return new GroovyGremlinSteps("__");
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> V() {
        g.append(chain("V"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> addE(String edgeLabel) {
        g.append(chain("addE", edgeLabel));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> addV() {
        g.append(chain("addV"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> addV(String vertexLabel) {
        g.append(chain("addV", vertexLabel));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> aggregate(String sideEffectKey) {
        g.append(chain("aggregate", sideEffectKey));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> and(GremlinSteps<String, GroovyPredicate>... andTraversals) {
        g.append(chain("and", traversals(andTraversals)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> as(String stepLabel) {
        g.append(chain("as", stepLabel));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> barrier() {
        g.append(chain("barrier"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> bothE(String... edgeLabels) {
        g.append(chain("bothE", (Object[]) edgeLabels));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> by(GremlinSteps<String, GroovyPredicate> traversal) {
        g.append(chain("by", traversal(traversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> by(GremlinSteps<String, GroovyPredicate> traversal,
                                                    Order order) {
        g.append(chain("by", traversal(traversal), order));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> cap(String sideEffectKey) {
        g.append(chain("cap", sideEffectKey));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> choose(GremlinSteps<String, GroovyPredicate> predicate,
                                                        GremlinSteps<String, GroovyPredicate> trueChoice,
                                                        GremlinSteps<String, GroovyPredicate> falseChoice) {
        g.append(chain("choose", traversal(predicate), traversal(trueChoice), traversal(falseChoice)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> choose(GroovyPredicate predicate,
                                                        GremlinSteps<String, GroovyPredicate> trueChoice,
                                                        GremlinSteps<String, GroovyPredicate> falseChoice) {
        g.append(chain("choose", predicate, traversal(trueChoice), traversal(falseChoice)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> choose(GroovyPredicate predicate,
                                                        GremlinSteps<String, GroovyPredicate> trueChoice) {
        g.append(chain("choose", predicate, traversal(trueChoice)));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> coalesce(GremlinSteps<String, GroovyPredicate>... coalesceTraversals) {
        g.append(chain("coalesce", traversals(coalesceTraversals)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> constant(Object e) {
        g.append(chain("constant", e));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> count() {
        g.append(chain("count"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> count(Scope scope) {
        g.append(chain("count", scope));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> dedup() {
        g.append(chain("dedup"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> drop() {
        g.append(chain("drop"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> emit() {
        g.append(chain("emit"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> fold() {
        g.append(chain("fold"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> from(String fromStepLabel) {
        g.append(chain("from", fromStepLabel));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> group() {
        g.append(chain("group"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> has(String propertyKey) {
        g.append(chain("has", propertyKey));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> has(String propertyKey, GroovyPredicate predicate) {
        g.append(chain("has", propertyKey, predicate));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> hasKey(String... labels) {
        g.append(chain("hasKey", (Object[]) labels));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> hasLabel(String... labels) {
        g.append(chain("hasLabel", (Object[]) labels));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> hasNot(String propertyKey) {
        g.append(chain("hasNot", propertyKey));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> id() {
        g.append(chain("id"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> identity() {
        g.append(chain("identity"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> inE(String... edgeLabels) {
        g.append(chain("inE", (Object[]) edgeLabels));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> inV() {
        g.append(chain("inV"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> inject(Object... injections) {
        g.append(chain("inject", injections));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> is(GroovyPredicate predicate) {
        g.append(chain("is", predicate));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> key() {
        g.append(chain("key"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> label() {
        g.append(chain("label"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> limit(long limit) {
        g.append(chain("limit", limit));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> local(GremlinSteps<String, GroovyPredicate> localTraversal) {
        g.append(chain("local", traversal(localTraversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> loops() {
        g.append(chain("loops"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> map(CustomFunction function) {
        g.append(chain(
            "map",
            Verbatim.of(
                apply(function.getName(), function.getArgs())
            )
        ));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> map(GremlinSteps<String, GroovyPredicate> traversal) {
        g.append(chain("map", traversal(traversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> max() {
        g.append(chain("max"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> mean() {
        g.append(chain("mean"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> min() {
        g.append(chain("min"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> not(GremlinSteps<String, GroovyPredicate> notTraversal) {
        g.append(chain("not", traversal(notTraversal)));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> or(GremlinSteps<String, GroovyPredicate>... orTraversals) {
        g.append(chain("or", traversals(orTraversals)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> order() {
        g.append(chain("order"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> otherV() {
        g.append(chain("otherV"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> outE(String... edgeLabels) {
        g.append(chain("outE", (Object[]) edgeLabels));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> outV() {
        g.append(chain("outV"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> path() {
        g.append(chain("path"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> properties(String... propertyKeys) {
        g.append(chain("properties", (Object[]) propertyKeys));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> property(String key, Object value) {
        g.append(chain("property", key, value));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> property(String key, GremlinSteps<String, GroovyPredicate> traversal) {
        g.append(chain("property", key, traversal(traversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> project(String... keys) {
        g.append(chain("project", (Object[]) keys));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> repeat(GremlinSteps<String, GroovyPredicate> repeatTraversal) {
        g.append(chain("repeat", traversal(repeatTraversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> select(String... selectKeys) {
        g.append(chain("select", (Object[]) selectKeys));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> select(Column column) {
        g.append(chain("select", column));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> sideEffect(GremlinSteps<String, GroovyPredicate> sideEffectTraversal) {
        g.append(chain("sideEffect", traversal(sideEffectTraversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> skip(long skip) {
        g.append(chain("skip", skip));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> sum() {
        g.append(chain("sum"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> times(int maxLoops) {
        g.append(chain("times", maxLoops));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> to(String toStepLabel) {
        g.append(chain("to", toStepLabel));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> unfold() {
        g.append(chain("unfold"));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> union(GremlinSteps<String, GroovyPredicate>... unionTraversals) {
        g.append(chain("union", traversals(unionTraversals)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> until(GremlinSteps<String, GroovyPredicate> untilTraversal) {
        g.append(chain("until", traversal(untilTraversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> value() {
        g.append(chain("value"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> valueMap() {
        g.append(chain("valueMap"));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> valueMap(boolean includeTokens) {
        g.append(chain("valueMap", includeTokens));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> values(String... propertyKeys) {
        g.append(chain("values", (Object[]) propertyKeys));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> where(GremlinSteps<String, GroovyPredicate> whereTraversal) {
        g.append(chain("where", traversal(whereTraversal)));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> where(GroovyPredicate predicate) {
        g.append(chain("where", predicate));
        return this;
    }

    private static Object traversal(GremlinSteps<String, GroovyPredicate> gremlinStep) {
        return Verbatim.of(gremlinStep.current());
    }

    private static Object[] traversals(GremlinSteps<String, GroovyPredicate>[] gremlinSteps) {
        return Stream.of(gremlinSteps)
            .map(GroovyGremlinSteps::traversal)
            .toArray(Object[]::new);
    }
}
