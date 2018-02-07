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
package org.opencypher.gremlin.translation.helpers;

import org.assertj.core.api.Assertions;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinPredicates;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class CypherAstAssertions extends Assertions {
    public static CypherAstAssert assertThat(CypherAstWrapper actual) {
        return new CypherAstAssert(actual);
    }

    public static CypherAstAssert assertThat(CypherAstWrapper actual, TranslatorFlavor<String, GroovyPredicate> flavor) {
        return new CypherAstAssert(actual, flavor);
    }

    public static final GroovyGremlinPredicates P = new GroovyGremlinPredicates();

    public static class __ {

        @SafeVarargs
        public static GremlinSteps<String, GroovyPredicate> and(GremlinSteps<String, GroovyPredicate>... ands) {
            return start().and(ands);
        }

        public static GremlinSteps<String, GroovyPredicate> constant(Object e) {
            return start().constant(e);
        }

        public static GremlinSteps<String, GroovyPredicate> not(GremlinSteps<String, GroovyPredicate> rhs) {
            return start().not(rhs);
        }

        @SafeVarargs
        public static GremlinSteps<String, GroovyPredicate> or(GremlinSteps<String, GroovyPredicate>... ors) {
            return start().or(ors);
        }

        public static GremlinSteps<String, GroovyPredicate> is(GroovyPredicate predicate) {
            return start().is(predicate);
        }

        public static GremlinSteps<String, GroovyPredicate> identity() {
            return start().identity();
        }


        public static GremlinSteps<String, GroovyPredicate> choose(GroovyPredicate predicate,
                                                                   GremlinSteps<String, GroovyPredicate> one,
                                                                   GremlinSteps<String, GroovyPredicate> two) {
            return start().choose(predicate, one, two);
        }

        @SafeVarargs
        public static GremlinSteps<String, GroovyPredicate> coalesce(GremlinSteps<String, GroovyPredicate>... traversals) {
            return start().coalesce(traversals);
        }


        public static GremlinSteps<String, GroovyPredicate> outE() {
            return start().outE();
        }

        public static GremlinSteps<String, GroovyPredicate> path() {
            return start().path();
        }

        public static GremlinSteps<String, GroovyPredicate> properties(String... propertyKeys) {
            return start().properties(propertyKeys);
        }

        public static GremlinSteps<String, GroovyPredicate> select(String... stepLabels) {
            return start().select(stepLabels);
        }

        public static GremlinSteps<String, GroovyPredicate> start() {
            return translationBuilder().start();
        }

        public static GremlinSteps<String, GroovyPredicate> V() {
            return translationBuilder().V();
        }

        public static GremlinSteps<String, GroovyPredicate> addV() {
            return translationBuilder().addV();
        }

        public static GremlinSteps<String, GroovyPredicate> addV(String vertexLabel) {
            return translationBuilder().addV(vertexLabel);
        }

        public static GremlinSteps<String, GroovyPredicate> inject(Object... starts) {
            return translationBuilder().inject(starts);
        }

        public static GremlinSteps<String, GroovyPredicate> loops() {
            return start().loops();
        }

        public static GremlinSteps<String, GroovyPredicate> project(String... keys) {
            return start().project(keys);
        }

        public static GremlinSteps<String, GroovyPredicate> values(String... keys) {
            return start().values(keys);
        }

        public static GremlinSteps<String, GroovyPredicate> as(String key) {
            return start().as(key);
        }

        public static GremlinSteps<String, GroovyPredicate> fold() {
            return start().fold();
        }

        public static GremlinSteps<String, GroovyPredicate> unfold() {
            return start().unfold();
        }

        private static GremlinSteps<String, GroovyPredicate> translationBuilder() {
            return Translator.builder().gremlinGroovy().build().translationBuilder();
        }
    }
}
