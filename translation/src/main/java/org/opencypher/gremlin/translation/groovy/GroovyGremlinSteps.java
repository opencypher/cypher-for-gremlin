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
    public GremlinSteps<String, GroovyPredicate> aggregate(String label) {
        g.append(chain("aggregate", label));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> and(GremlinSteps<String, GroovyPredicate>... ands) {
        g.append(chain("and", (Object[]) ands));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> as(String label) {
        g.append(chain("as", label));
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
        g.append(chain("by", traversal));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> by(GremlinSteps<String, GroovyPredicate> traversal,
                                                    Order order) {
        g.append(chain("by", traversal, order));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> cap(String label) {
        g.append(chain("cap", label));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> choose(GremlinSteps<String, GroovyPredicate> predicate,
                                                        GremlinSteps<String, GroovyPredicate> trueChoice,
                                                        GremlinSteps<String, GroovyPredicate> falseChoice) {
        g.append(chain("choose", predicate, trueChoice, falseChoice));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> choose(GroovyPredicate predicate, GremlinSteps<String, GroovyPredicate> trueChoice, GremlinSteps<String, GroovyPredicate> falseChoice) {
        g.append(chain("choose", predicate, trueChoice, falseChoice));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> choose(GroovyPredicate predicate, GremlinSteps<String, GroovyPredicate> trueChoice) {
        g.append(chain("choose", predicate, trueChoice));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> coalesce(GremlinSteps<String, GroovyPredicate>... traversals) {
        g.append(chain("coalesce", (Object[]) traversals));
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
    public GremlinSteps<String, GroovyPredicate> from(String stepLabel) {
        g.append(chain("from", stepLabel));
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
    public GremlinSteps<String, GroovyPredicate> hasKey(String... key) {
        g.append(chain("hasKey", (Object[]) key));
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
    public GremlinSteps<String, GroovyPredicate> not(GremlinSteps<String, GroovyPredicate> rhs) {
        g.append(chain("not", rhs));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> or(GremlinSteps<String, GroovyPredicate>... ors) {
        g.append(chain("or", (Object[]) ors));
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
    public GremlinSteps<String, GroovyPredicate> property(String key, GremlinSteps<String, GroovyPredicate> builder) {
        g.append(chain("property", key, Verbatim.of(builder.current())));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> project(String... keys) {
        g.append(chain("project", (Object[]) keys));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> repeat(GremlinSteps<String, GroovyPredicate> gremlinSteps) {
        g.append(chain("repeat", gremlinSteps));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> select(String... stepLabels) {
        g.append(chain("select", (Object[]) stepLabels));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> select(Column column) {
        g.append(chain("select", column));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> sideEffect(GremlinSteps<String, GroovyPredicate> gremlinSteps) {
        g.append(chain("sideEffect", gremlinSteps));
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
    public GremlinSteps<String, GroovyPredicate> to(String stepLabel) {
        g.append(chain("to", stepLabel));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> unfold() {
        g.append(chain("unfold"));
        return this;
    }

    @SafeVarargs
    @Override
    public final GremlinSteps<String, GroovyPredicate> union(GremlinSteps<String, GroovyPredicate>... gremlinSteps) {
        g.append(chain("union", (Object[]) gremlinSteps));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> until(GremlinSteps<String, GroovyPredicate> gremlinSteps) {
        g.append(chain("until", gremlinSteps));
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
    public GremlinSteps<String, GroovyPredicate> values(String... propertyKeys) {
        g.append(chain("values", (Object[]) propertyKeys));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> where(GremlinSteps<String, GroovyPredicate> gremlinSteps) {
        g.append(chain("where", gremlinSteps));
        return this;
    }

    @Override
    public GremlinSteps<String, GroovyPredicate> where(GroovyPredicate predicate) {
        g.append(chain("where", predicate));
        return this;
    }
}
