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
import org.opencypher.gremlin.translation.TranslationBuilder;
import org.opencypher.gremlin.translation.TranslatorFactory;
import org.opencypher.gremlin.translation.string.StringPredicate;
import org.opencypher.gremlin.translation.string.StringPredicateFactory;

public class CypherAstAssertions extends Assertions {
    public static CypherAstAssert assertThat(CypherAstWrapper actual) {
        return new CypherAstAssert(actual);
    }

    public static final StringPredicateFactory P = new StringPredicateFactory();

    public static class __ {

        @SafeVarargs
        public static TranslationBuilder<String, StringPredicate> and(TranslationBuilder<String, StringPredicate>... ands) {
            return start().and(ands);
        }

        public static TranslationBuilder<String, StringPredicate> constant(Object e) {
            return start().constant(e);
        }

        public static TranslationBuilder<String, StringPredicate> not(TranslationBuilder<String, StringPredicate> rhs) {
            return start().not(rhs);
        }

        @SafeVarargs
        public static TranslationBuilder<String, StringPredicate> or(TranslationBuilder<String, StringPredicate>... ors) {
            return start().or(ors);
        }

        public static TranslationBuilder<String, StringPredicate> is(StringPredicate predicate) {
            return start().is(predicate);
        }


        public static TranslationBuilder<String, StringPredicate> choose(StringPredicate predicate,
                                                                         TranslationBuilder<String, StringPredicate> one,
                                                                         TranslationBuilder<String, StringPredicate> two) {
            return start().choose(predicate, one, two);
        }

        @SafeVarargs
        public static TranslationBuilder<String, StringPredicate> coalesce(TranslationBuilder<String, StringPredicate>... traversals) {
            return start().coalesce(traversals);
        }


        public static TranslationBuilder<String, StringPredicate> outE() {
            return start().outE();
        }

        public static TranslationBuilder<String, StringPredicate> path() {
            return start().path();
        }

        public static TranslationBuilder<String, StringPredicate> properties(String... propertyKeys) {
            return start().properties(propertyKeys);
        }

        public static TranslationBuilder<String, StringPredicate> select(String... stepLabels) {
            return start().select(stepLabels);
        }

        public static TranslationBuilder<String, StringPredicate> start() {
            return translationBuilder().start();
        }

        public static TranslationBuilder<String, StringPredicate> V() {
            return translationBuilder().V();
        }

        public static TranslationBuilder<String, StringPredicate> addV() {
            return translationBuilder().addV();
        }

        public static TranslationBuilder<String, StringPredicate> addV(String vertexLabel) {
            return translationBuilder().addV(vertexLabel);
        }

        public static TranslationBuilder<String, StringPredicate> inject(Object... starts) {
            return translationBuilder().inject(starts);
        }

        public static TranslationBuilder<String, StringPredicate> project(String... keys) {
            return start().project(keys);
        }

        public static TranslationBuilder<String, StringPredicate> values(String... keys) {
            return start().values(keys);
        }

        public static TranslationBuilder<String, StringPredicate> as(String key) {
            return start().as(key);
        }

        public static TranslationBuilder<String, StringPredicate> fold() {
            return start().fold();
        }

        public static TranslationBuilder<String, StringPredicate> unfold() {
            return start().unfold();
        }

        private static TranslationBuilder<String, StringPredicate> translationBuilder() {
            return TranslatorFactory.string().translationBuilder();
        }
    }
}
