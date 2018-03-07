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
import org.opencypher.gremlin.translation.GremlinBindings;
import org.opencypher.gremlin.translation.GremlinPredicates;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinBindings;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinPredicates;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.traversal.TraversalGremlinBindings;
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
    private final GremlinBindings bindings;

    private Translator(GremlinSteps<T, P> steps,
                       GremlinPredicates<P> predicates,
                       GremlinBindings bindings,
                       TranslatorFlavor<T, P> flavor) {
        this.steps = flavor.decorateTranslationBuilder(steps);
        this.predicates = predicates;
        this.bindings = bindings;
    }

    /**
     * Provides access to the traversal DSL.
     *
     * @return traversal DSL
     * @see #predicates()
     * @see #bindings()
     */
    public GremlinSteps<T, P> steps() {
        return steps;
    }

    /**
     * Returns a factory for traversal predicates for use with the traversal DSL.
     *
     * @return factory for traversal predicates
     * @see #steps()
     * @see #bindings()
     */
    public GremlinPredicates<P> predicates() {
        return predicates;
    }

    /**
     * Returns a strategy for working with query bindings.
     *
     * @return strategy for query bindings
     * @see #steps()
     * @see #predicates()
     */
    public GremlinBindings bindings() {
        return bindings;
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
         * @return builder for translator to Gremlin-Groovy string
         */
        public GremlinGroovyFlavorBuilder gremlinGroovy() {
            return new GremlinGroovyFlavorBuilder(
                new GroovyGremlinSteps(),
                new GroovyGremlinPredicates(),
                new GroovyGremlinBindings()
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
                new TraversalGremlinBindings()
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
            GremlinBindings parameters
        ) {
            return new FlavorBuilder<>(
                steps,
                predicates,
                parameters
            );
        }
    }

    public static class FlavorBuilder<T, P> {
        private final GremlinSteps<T, P> steps;
        private final GremlinPredicates<P> predicates;
        protected GremlinBindings bindings;

        private FlavorBuilder(GremlinSteps<T, P> steps,
                              GremlinPredicates<P> predicates,
                              GremlinBindings bindings) {
            this.steps = steps;
            this.predicates = predicates;
            this.bindings = bindings;
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
                bindings,
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
                bindings,
                flavor
            );
        }
    }

    public static final class GremlinGroovyFlavorBuilder extends FlavorBuilder<String, GroovyPredicate> {
        private GremlinGroovyFlavorBuilder(GremlinSteps<String, GroovyPredicate> steps,
                                           GremlinPredicates<GroovyPredicate> predicates,
                                           GremlinBindings bindings) {
            super(steps, predicates, bindings);
        }

        /**
         * Builds a {@link Translator} that inlines query parameters.
         *
         * @return builder for translator to Gremlin-Groovy string
         */
        public FlavorBuilder<String, GroovyPredicate> inlineParameters() {
            bindings = new TraversalGremlinBindings();
            return this;
        }
    }
}
