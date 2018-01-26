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


import java.util.Set;
import java.util.function.Function;

/**
 * Abstraction over the process of building a translation
 * and result post-processing configuration
 * used in Cypher AST walkers.
 */
public class Translator<T, P> {
    private final TranslationBuilder<T, P> translationBuilder;
    private final PredicateFactory<P> predicateFactory;
    private final Function<TranslationBuilder<T, P>, T> translationProducer;

    Translator(TranslationBuilder<T, P> translationBuilder,
               PredicateFactory<P> predicateFactory,
               Function<TranslationBuilder<T, P>, T> translationProducer) {
        this.translationBuilder = translationBuilder;
        this.predicateFactory = predicateFactory;
        this.translationProducer = translationProducer;

    }

    Translator(TranslationBuilder<T, P> translationBuilder,
               PredicateFactory<P> predicateFactory) {
        this(translationBuilder, predicateFactory, TranslationBuilder::current);
    }

    /**
     * Provides access to a traversal DSL.
     *
     * @return traversal DSL
     */
    public TranslationBuilder<T, P> translationBuilder() {
        return translationBuilder;
    }

    /**
     * Return a factory for traversal predicates for use in a traversal DSL.
     *
     * @return factory for traversal predicates
     */
    public PredicateFactory<P> predicateFactory() {
        return predicateFactory;
    }

    /**
     * Creates a translation plan.
     *
     * @param options enabled Cypher query options
     * @return translation plan
     */
    public TranslationPlan<T> toTranslationPlan(Set<StatementOption> options) {
        T translation = translationProducer.apply(translationBuilder.copy());

        return new TranslationPlan<>(
            translation,
            options
        );
    }
}
