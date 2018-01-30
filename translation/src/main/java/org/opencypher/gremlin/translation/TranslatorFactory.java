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

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.opencypher.gremlin.translation.string.StringPredicate;
import org.opencypher.gremlin.translation.string.StringPredicateFactory;
import org.opencypher.gremlin.translation.string.StringTranslationBuilder;
import org.opencypher.gremlin.translation.traversal.TraversalPredicateFactory;
import org.opencypher.gremlin.translation.traversal.TraversalTranslationBuilder;

import java.util.function.Function;

import static org.opencypher.gremlin.traversal.GremlinRemote.transposeReturnMap;

/**
 * This factory creates {@link Translator} instances with common configurations.
 * <p>
 * Translators produced by this factory can be used to translate a single query.
 */
public class TranslatorFactory {
    private TranslatorFactory() {
    }

    /**
     * Creates a {@link Translator} that translates Cypher queries
     * to strings of Gremlin-Groovy.
     *
     * @return translator to string
     */
    public static Translator<String, StringPredicate> string() {
        return new Translator<>(new StringTranslationBuilder(), new StringPredicateFactory());
    }

    /**
     * Creates a {@link Translator} that translates Cypher queries
     * to native Gremlin {@code Traversal} that can be iterated to yield results
     * for its source.
     * <p>
     * See: https://tinkerpop.apache.org/docs/current/reference/#traversal
     *
     * @param g traversal to modify with translated steps
     * @return provided traversal, modified
     */
    public static Translator<GraphTraversal, P> traversal(GraphTraversal g) {
        return new Translator<>(new TraversalTranslationBuilder(g), new TraversalPredicateFactory());
    }

    static Translator<String, StringPredicate> transposedString() {
        return new Translator<>(
            new StringTranslationBuilder(),
            new StringPredicateFactory(),
            transposeReturnMap());
    }

    public static Translator<String, StringPredicate> cosmos() {
        return new Translator<>(
            new CosmosStringTranslationBuilder("g", null),
            new StringPredicateFactory(),
            transposeReturnMap()
        );
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
