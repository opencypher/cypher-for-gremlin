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
package org.opencypher.gremlin.translation.translator;


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.opencypher.gremlin.translation.GremlinPredicates;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinPredicates;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.traversal.TraversalGremlinPredicates;
import org.opencypher.gremlin.translation.traversal.TraversalGremlinSteps;

/**
 * Abstraction over the process of building a translation
 * for different targets.
 */
public final class Translator<T, P> {
    private final GremlinSteps<T, P> gremlinSteps;
    private final GremlinPredicates<P> gremlinPredicates;

    private Translator(GremlinSteps<T, P> gremlinSteps,
                       GremlinPredicates<P> gremlinPredicates,
                       TranslatorFlavor<T, P> flavor) {
        this.gremlinSteps = flavor.decorateTranslationBuilder(gremlinSteps);
        this.gremlinPredicates = gremlinPredicates;
    }

    /**
     * Provides access to the traversal DSL.
     *
     * @return traversal DSL
     * @see #predicateFactory()
     */
    public GremlinSteps<T, P> translationBuilder() {
        return gremlinSteps;
    }

    /**
     * Returns a factory for traversal predicates for use with the traversal DSL.
     *
     * @return factory for traversal predicates
     * @see #translationBuilder()
     */
    public GremlinPredicates<P> predicateFactory() {
        return gremlinPredicates;
    }

    /**
     * Creates a translation for the configured target.
     *
     * @return translation
     */
    public T translate() {
        return gremlinSteps.copy().current();
    }

    /**
     * Starts to build a translator.
     *
     * @return translator builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Builder() {
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to strings of Gremlin-Groovy.
         *
         * @return builder for translator to Gremlin Groovy string
         */
        public FlavorBuilder<String, GroovyPredicate> gremlinGroovy() {
            return new FlavorBuilder<>(
                new GroovyGremlinSteps(),
                new GroovyGremlinPredicates()
            );
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to native Gremlin {@code Traversal} that can be iterated to yield results
         * for its source.
         * <p>
         * See: https://tinkerpop.apache.org/docs/current/reference/#traversal
         *
         * @param g traversal to modify with translated steps
         * @return builder for translator to Gremlin traversal
         */
        public FlavorBuilder<GraphTraversal, org.apache.tinkerpop.gremlin.process.traversal.P> traversal(GraphTraversal g) {
            return new FlavorBuilder<>(
                new TraversalGremlinSteps(g),
                new TraversalGremlinPredicates()
            );
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to custom format via the provided steps and predicates implementation.
         *
         * @param gremlinSteps      Gremlin steps implementation
         * @param gremlinPredicates Gremlin predicates implementation
         * @param <T>               translation target type
         * @param <P>               predicate target type
         * @return builder for translator to custom format
         */
        public <T, P> FlavorBuilder<T, P> custom(
            GremlinSteps<T, P> gremlinSteps,
            GremlinPredicates<P> gremlinPredicates
        ) {
            return new FlavorBuilder<>(
                gremlinSteps,
                gremlinPredicates
            );
        }
    }

    public static final class FlavorBuilder<T, P> {
        private final GremlinSteps<T, P> gremlinSteps;
        private final GremlinPredicates<P> gremlinPredicates;

        private FlavorBuilder(GremlinSteps<T, P> gremlinSteps,
                              GremlinPredicates<P> gremlinPredicates) {
            this.gremlinSteps = gremlinSteps;
            this.gremlinPredicates = gremlinPredicates;
        }

        /**
         * Builds a {@link Translator}.
         *
         * @return translator
         */
        public Translator<T, P> build() {
            return new Translator<>(
                gremlinSteps,
                gremlinPredicates,
                TranslatorFlavor.gremlinServer()
            );
        }

        /**
         * Builds a {@link Translator} with the given translator flavor.
         *
         * @param flavor translator flavor
         * @return translator
         */
        public Translator<T, P> build(TranslatorFlavor<T, P> flavor) {
            return new Translator<>(
                gremlinSteps,
                gremlinPredicates,
                flavor
            );
        }
    }
}
