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


import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.opencypher.gremlin.translation.GremlinBindings;
import org.opencypher.gremlin.translation.GremlinPredicates;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.bytecode.BytecodeGremlinBindings;
import org.opencypher.gremlin.translation.bytecode.BytecodeGremlinPredicates;
import org.opencypher.gremlin.translation.bytecode.BytecodeGremlinSteps;
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
                       GremlinBindings bindings) {
        this.steps = steps;
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
        public ParametrizedConfiguredBuilder<String, GroovyPredicate> gremlinGroovy() {
            return new ParametrizedConfiguredBuilder<>(
                new GroovyGremlinSteps(),
                new GroovyGremlinPredicates(),
                new GroovyGremlinBindings()
            );
        }

        /**
         * Builds a {@link Translator} that translates Cypher queries
         * to Gremlin bytecode.
         *
         * @return builder for translator to Gremlin bytecode
         */
        public ParametrizedConfiguredBuilder<Bytecode, org.apache.tinkerpop.gremlin.process.traversal.P> bytecode() {
            return new ParametrizedConfiguredBuilder<>(
                new BytecodeGremlinSteps(),
                new BytecodeGremlinPredicates(),
                new BytecodeGremlinBindings()
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
        public ConfiguredBuilder<GraphTraversal, org.apache.tinkerpop.gremlin.process.traversal.P> traversal() {
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
        public ConfiguredBuilder<GraphTraversal, org.apache.tinkerpop.gremlin.process.traversal.P> traversal(GraphTraversal g) {
            return new ConfiguredBuilder<>(
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
        public <T, P> ConfiguredBuilder<T, P> custom(
            GremlinSteps<T, P> steps,
            GremlinPredicates<P> predicates,
            GremlinBindings parameters
        ) {
            return new ConfiguredBuilder<>(
                steps,
                predicates,
                parameters
            );
        }
    }

    public static class ConfiguredBuilder<T, P> {
        private final GremlinSteps<T, P> steps;
        private final GremlinPredicates<P> predicates;
        protected GremlinBindings bindings;

        private ConfiguredBuilder(GremlinSteps<T, P> steps,
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
                bindings
            );
        }
    }

    public static final class ParametrizedConfiguredBuilder<T, P> extends ConfiguredBuilder<T, P> {
        private ParametrizedConfiguredBuilder(GremlinSteps<T, P> steps,
                                              GremlinPredicates<P> predicates,
                                              GremlinBindings bindings) {
            super(steps, predicates, bindings);
        }

        /**
         * Builds a {@link Translator} that inlines query parameters.
         *
         * @return builder for translator
         */
        public ConfiguredBuilder<T, P> inlineParameters() {
            bindings = new TraversalGremlinBindings();
            return this;
        }
    }
}
