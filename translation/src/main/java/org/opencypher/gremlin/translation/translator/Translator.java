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


import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.opencypher.gremlin.translation.GremlinParameters;
import org.opencypher.gremlin.translation.GremlinPredicates;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinParameters;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinPredicates;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.traversal.TraversalGremlinParameters;
import org.opencypher.gremlin.translation.traversal.TraversalGremlinPredicates;
import org.opencypher.gremlin.translation.traversal.TraversalGremlinSteps;

/**
 * Abstraction over the process of building a translation
 * for different targets.
 * <p>
 * Translator instances are not reusable.
 */
public final class Translator<T, P> {
    private final GremlinSteps<T, P> steps;
    private final GremlinPredicates<P> predicates;
    private final GremlinParameters parameters;

    private Translator(GremlinSteps<T, P> steps,
                       GremlinPredicates<P> predicates,
                       GremlinParameters parameters,
                       TranslatorFlavor<T, P> flavor) {
        this.steps = flavor.decorateTranslationBuilder(steps);
        this.predicates = predicates;
        this.parameters = parameters;
    }

    /**
     * Provides access to the traversal DSL.
     *
     * @return traversal DSL
     * @see #predicates()
     * @see #parameters()
     */
    public GremlinSteps<T, P> steps() {
        return steps;
    }

    /**
     * Returns a factory for traversal predicates for use with the traversal DSL.
     *
     * @return factory for traversal predicates
     * @see #steps()
     * @see #parameters()
     */
    public GremlinPredicates<P> predicates() {
        return predicates;
    }

    /**
     * Returns a strategy for working with query parameters.
     *
     * @return strategy for query parameters
     * @see #steps()
     * @see #predicates()
     */
    public GremlinParameters parameters() {
        return parameters;
    }

    /**
     * Creates a translation for the configured target.
     *
     * @return translation
     */
    public T translate() {
        return steps.current();
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
            return gremlinGroovy(false);
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to strings of Gremlin-Groovy.
         *
         * @param inlineParameters if true, inline provided parameter values
         * @return builder for translator to Gremlin Groovy string
         */
        public FlavorBuilder<String, GroovyPredicate> gremlinGroovy(boolean inlineParameters) {
            return new FlavorBuilder<>(
                new GroovyGremlinSteps(),
                new GroovyGremlinPredicates(),
                inlineParameters ? new TraversalGremlinParameters() : new GroovyGremlinParameters()
            );
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to Gremlin anonymous {@code GraphTraversal}.
         * <p>
         * See: https://tinkerpop.apache.org/docs/current/reference/#traversal
         *
         * @return builder for translator to Gremlin traversal
         */
        public FlavorBuilder<GraphTraversal, org.apache.tinkerpop.gremlin.process.traversal.P> traversal() {
            return traversal(new DefaultGraphTraversal());
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to Gremlin {@code GraphTraversal} that can be iterated to yield results
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
                new TraversalGremlinPredicates(),
                new TraversalGremlinParameters()
            );
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to custom format via the provided steps and predicates implementation.
         *
         * @param steps      Gremlin steps implementation
         * @param predicates Gremlin predicates implementation
         * @param parameters Parameters strategy implementation
         * @param <T>        translation target type
         * @param <P>        predicate target type
         * @return builder for translator to custom format
         */
        public <T, P> FlavorBuilder<T, P> custom(
            GremlinSteps<T, P> steps,
            GremlinPredicates<P> predicates,
            GremlinParameters parameters
        ) {
            return new FlavorBuilder<>(
                steps,
                predicates,
                parameters
            );
        }
    }

    public static final class FlavorBuilder<T, P> {
        private final GremlinSteps<T, P> steps;
        private final GremlinPredicates<P> predicates;
        private final GremlinParameters parameters;

        private FlavorBuilder(GremlinSteps<T, P> steps,
                              GremlinPredicates<P> predicates,
                              GremlinParameters parameters) {
            this.steps = steps;
            this.predicates = predicates;
            this.parameters = parameters;
        }

        /**
         * Builds a {@link Translator}.
         *
         * @return translator
         */
        public Translator<T, P> build() {
            return new Translator<>(
                steps,
                predicates,
                parameters,
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
                steps,
                predicates,
                parameters,
                flavor
            );
        }
    }
}
