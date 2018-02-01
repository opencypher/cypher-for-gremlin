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
package org.opencypher.gremlin.translation.string;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.opencypher.gremlin.translation.AliasHistory;
import org.opencypher.gremlin.translation.Tokens;
import org.opencypher.gremlin.translation.TranslationBuilder;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.opencypher.gremlin.translation.string.StringTranslationUtils.chain;
import static org.opencypher.gremlin.translation.string.StringTranslationUtils.unquoted;

public class StringTranslationBuilder implements TranslationBuilder<String, StringPredicate> {

    private final StringBuilder g;
    private StringTranslationBuilder parent;
    private AliasHistory aliasHistory;

    public StringTranslationBuilder() {
        this("g", null);
    }

    protected StringTranslationBuilder(String start, StringTranslationBuilder parent) {
        g = new StringBuilder(start);
        this.parent = parent;
        this.aliasHistory = parent != null ? parent.aliasHistory.copy() : new AliasHistory();
    }

    @Override
    public StringTranslationBuilder copy() {
        return new StringTranslationBuilder(g.toString(), parent);
    }

    @Override
    public TranslationBuilder<String, StringPredicate> mutate(Consumer<TranslationBuilder<String, StringPredicate>> mutator) {
        mutator.accept(this);
        return this;
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
    public String alias(String label) {
        return aliasHistory.current(label);
    }

    @Override
    public TranslationBuilder<String, StringPredicate> start() {
        return new StringTranslationBuilder("__", this);
    }

    @Override
    public TranslationBuilder<String, StringPredicate> V() {
        g.append(chain("V"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> addE(String edgeLabel) {
        g.append(chain("addE", edgeLabel));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> addV() {
        g.append(chain("addV"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> addV(String vertexLabel) {
        g.append(chain("addV", vertexLabel));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> aggregate(String label) {
        g.append(chain("aggregate", label));
        return this;
    }

    @SafeVarargs
    @Override
    public final TranslationBuilder<String, StringPredicate> and(TranslationBuilder<String, StringPredicate>... ands) {
        g.append(chain("and", (Object[]) ands));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> as(String label) {
        g.append(chain("as", aliasHistory.next(label)));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> barrier() {
        g.append(chain("barrier"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> bothE(String... edgeLabels) {
        g.append(chain("bothE", (Object[]) edgeLabels));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> by(TranslationBuilder<String, StringPredicate> traversal) {
        g.append(chain("by", traversal));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> by(TranslationBuilder<String, StringPredicate> traversal,
                                                          Order order) {
        g.append(chain("by", traversal, order));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> choose(TranslationBuilder<String, StringPredicate> predicate,
                                                              TranslationBuilder<String, StringPredicate> trueChoice,
                                                              TranslationBuilder<String, StringPredicate> falseChoice) {
        g.append(chain("choose", predicate, trueChoice, falseChoice));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> choose(StringPredicate predicate, TranslationBuilder<String, StringPredicate> trueChoice, TranslationBuilder<String, StringPredicate> falseChoice) {
        g.append(chain("choose", predicate, trueChoice, falseChoice));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> choose(StringPredicate predicate, TranslationBuilder<String, StringPredicate> trueChoice) {
        g.append(chain("choose", predicate, trueChoice));
        return this;
    }

    @SafeVarargs
    @Override
    public final TranslationBuilder<String, StringPredicate> coalesce(TranslationBuilder<String, StringPredicate>... traversals) {
        g.append(chain("coalesce", (Object[]) traversals));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> constant(Object e) {
        g.append(chain("constant", e));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> count() {
        g.append(chain("count"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> count(Scope scope) {
        g.append(chain("count", scope));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> dedup() {
        g.append(chain("dedup"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> drop() {
        g.append(chain("drop"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> emit() {
        g.append(chain("emit"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> fold() {
        g.append(chain("fold"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> from(String stepLabel) {
        g.append(chain("from", stepLabel));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> group() {
        g.append(chain("group"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> has(String propertyKey) {
        g.append(chain("has", propertyKey));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> hasKey(String... key) {
        g.append(chain("hasKey", (Object[]) key));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> hasLabel(String... labels) {
        g.append(chain("hasLabel", (Object[]) labels));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> hasNot(String propertyKey) {
        g.append(chain("hasNot", propertyKey));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> id() {
        g.append(chain("id"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> inE(String... edgeLabels) {
        g.append(chain("inE", (Object[]) edgeLabels));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> inV() {
        g.append(chain("inV"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> inject(Object... injections) {
        g.append(chain("inject", injections));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> is(StringPredicate predicate) {
        g.append(chain("is", predicate));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> key() {
        g.append(chain("key"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> label() {
        g.append(chain("label"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> limit(long limit) {
        g.append(chain("limit", limit));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> map(String functionName, Function<Traverser, Object> function) {
        g.append(".map(").append(functionName).append("())");
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> max() {
        g.append(chain("max"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> mean() {
        g.append(chain("mean"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> min() {
        g.append(chain("min"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> not(TranslationBuilder<String, StringPredicate> rhs) {
        g.append(chain("not", rhs));
        return this;
    }

    @SafeVarargs
    @Override
    public final TranslationBuilder<String, StringPredicate> or(TranslationBuilder<String, StringPredicate>... ors) {
        g.append(chain("or", (Object[]) ors));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> order() {
        g.append(chain("order"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> otherV() {
        g.append(chain("otherV"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> outE(String... edgeLabels) {
        g.append(chain("outE", (Object[]) edgeLabels));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> outV() {
        g.append(chain("outV"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> path() {
        g.append(chain("path"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> properties(String... propertyKeys) {
        g.append(chain("properties", (Object[]) propertyKeys));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> property(String key, Object value) {
        if (Tokens.NULL.equals(value)) {
            // FIXME Should only work like this SET
            sideEffect(start().properties(key).drop());
        } else {
            g.append(chain("property", key, value));
        }
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> project(String... keys) {
        g.append(chain("project", (Object[]) keys));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> propertyList(String key, Collection values) {
        if (values.isEmpty()) {
            // FIXME Should only work like this SET
            sideEffect(start().properties(key).drop());
        } else {
            for (Object value : values) {
                g.append(chain("property", unquoted("list"), key, value));
            }
        }
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> range(long low, long high) {
        g.append(chain("range", low, high));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> repeat(TranslationBuilder<String, StringPredicate> translationBuilder) {
        g.append(chain("repeat", translationBuilder));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> select(String... stepLabels) {
        String[] aliases = Stream.of(stepLabels)
            .map(aliasHistory::current)
            .toArray(String[]::new);
        return selectLabels(aliases);
    }

    @Override
    public TranslationBuilder<String, StringPredicate> selectLabels(String... stepLabels) {
        g.append(chain("select", (Object[]) stepLabels));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> select(Column column) {
        g.append(chain("select", column));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> sideEffect(TranslationBuilder<String, StringPredicate> translationBuilder) {
        g.append(chain("sideEffect", translationBuilder));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> skip(long skip) {
        g.append(chain("skip", skip));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> sum() {
        g.append(chain("sum"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> times(Integer maxLoops) {
        g.append(chain("times", maxLoops));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> to(String stepLabel) {
        g.append(chain("to", stepLabel));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> unfold() {
        g.append(chain("unfold"));
        return this;
    }

    @SafeVarargs
    @Override
    public final TranslationBuilder<String, StringPredicate> union(TranslationBuilder<String, StringPredicate>... translationBuilders) {
        g.append(chain("union", (Object[]) translationBuilders));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> until(TranslationBuilder<String, StringPredicate> translationBuilder) {
        g.append(chain("until", translationBuilder));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> value() {
        g.append(chain("value"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> valueMap() {
        g.append(chain("valueMap"));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> values(String... propertyKeys) {
        g.append(chain("values", (Object[]) propertyKeys));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> where(TranslationBuilder<String, StringPredicate> translationBuilder) {
        g.append(chain("where", translationBuilder));
        return this;
    }

    @Override
    public TranslationBuilder<String, StringPredicate> where(StringPredicate predicate) {
        g.append(chain("where", predicate));
        return this;
    }
}
