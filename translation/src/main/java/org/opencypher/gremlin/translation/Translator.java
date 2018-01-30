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


import java.util.function.Function;

/**
 * Abstraction over the process of building a translation
 * for different targets.
 *
 * @see TranslatorFactory
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
     * Provides access to the traversal DSL.
     *
     * @return traversal DSL
     * @see #predicateFactory()
     */
    public TranslationBuilder<T, P> translationBuilder() {
        return translationBuilder;
    }

    /**
     * Returns a factory for traversal predicates for use with the traversal DSL.
     *
     * @return factory for traversal predicates
     * @see #translationBuilder()
     */
    public PredicateFactory<P> predicateFactory() {
        return predicateFactory;
    }

    /**
     * Creates a translation for the configured target.
     *
     * @return translation
     */
    public T translate() {
        return translationProducer.apply(translationBuilder.copy());
    }
}
