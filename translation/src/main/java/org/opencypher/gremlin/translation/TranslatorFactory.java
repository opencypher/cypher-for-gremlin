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

import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.opencypher.gremlin.translation.string.StringPredicate;
import org.opencypher.gremlin.translation.string.StringPredicateFactory;
import org.opencypher.gremlin.translation.string.StringTranslationBuilder;
import org.opencypher.gremlin.translation.traversal.TraversalPredicateFactory;
import org.opencypher.gremlin.translation.traversal.TraversalTranslationBuilder;
import org.opencypher.gremlin.traversal.GremlinRemote;

import java.util.function.Function;

public class TranslatorFactory {
    private TranslatorFactory() {
    }

    public static Translator<String, StringPredicate> string() {
        return new Translator<>(new StringTranslationBuilder(), new StringPredicateFactory());
    }

    public static Translator<GraphTraversal, org.apache.tinkerpop.gremlin.process.traversal.P> traversal(GraphTraversal g) {
        return new Translator<>(new TraversalTranslationBuilder(g), new TraversalPredicateFactory());
    }

    public static Translator<String, StringPredicate> cosmos() {
        return new Translator<>(
            new CosmosStringTranslationBuilder("g", null),
            new StringPredicateFactory(),
            transposeReturnMap()
        );
    }

    private static Function<TranslationBuilder<String, StringPredicate>, String> transposeReturnMap() {
        return builder -> builder.project(GremlinRemote.PIVOTS, GremlinRemote.AGGREGATIONS)
            .by(builder.start().select(Column.keys))
            .by(builder.start().select(Column.values))
            .current();
    }

    private static class CosmosStringTranslationBuilder extends StringTranslationBuilder {
        CosmosStringTranslationBuilder(String start, StringTranslationBuilder parent) {
            super(start, parent);
        }

        @Override
        public TranslationBuilder<String, StringPredicate> start() {
            return new CosmosStringTranslationBuilder("__", this);
        }

        @Override
        public TranslationBuilder<String, StringPredicate> values(String... propertyKeys) {
            return properties()
                .hasKey(propertyKeys)
                .value();
        }

        @Override
        public TranslationBuilder<String, StringPredicate> map(String functionName, Function<Traverser, Object> function) {
            throw new IllegalArgumentException("Custom functions are not supported: " + functionName);
        }
    }
}
