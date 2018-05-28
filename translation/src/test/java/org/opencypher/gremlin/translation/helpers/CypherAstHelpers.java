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

import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Map;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.ir.builder.IRGremlinBindings;
import org.opencypher.gremlin.translation.ir.builder.IRGremlinPredicates;
import org.opencypher.gremlin.translation.ir.builder.IRGremlinSteps;
import org.opencypher.gremlin.translation.ir.model.GremlinPredicate;
import org.opencypher.gremlin.translation.ir.model.GremlinStep;
import org.opencypher.gremlin.translation.ir.rewrite.GremlinRewriter;
import org.opencypher.gremlin.translation.ir.verify.GremlinPostCondition;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;
import scala.collection.Seq;

public final class CypherAstHelpers {
    private CypherAstHelpers() {
    }

    public static CypherAstWrapper parse(String queryText) {
        return parse(queryText, emptyMap());
    }

    public static CypherAstWrapper parse(String queryText, Map<String, Object> passedParams) {
        try {
            return CypherAstWrapper.parse(queryText, passedParams);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing query: " + queryText, e);
        }
    }

    public static Object parameter(String name) {
        IRGremlinBindings parameters = new IRGremlinBindings();
        return parameters.bind(name, null);
    }

    public static TranslatorFlavor flavor(List<GremlinRewriter> rewriters, List<GremlinPostCondition> postConditions) {
        return null;
    }

    public static final IRGremlinPredicates P = new IRGremlinPredicates();

    public static class __ {

        @SafeVarargs
        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> and(GremlinSteps<Seq<GremlinStep>, GremlinPredicate>... ands) {
            return start().and(ands);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> constant(Object e) {
            return start().constant(e);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> not(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> rhs) {
            return start().not(rhs);
        }

        @SafeVarargs
        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> or(GremlinSteps<Seq<GremlinStep>, GremlinPredicate>... ors) {
            return start().or(ors);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> is(GremlinPredicate predicate) {
            return start().is(predicate);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> identity() {
            return start().identity();
        }


        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> choose(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversalPredicate,
                                                                              GremlinSteps<Seq<GremlinStep>, GremlinPredicate> trueChoice,
                                                                              GremlinSteps<Seq<GremlinStep>, GremlinPredicate> falseChoice) {
            return start().choose(traversalPredicate, trueChoice, falseChoice);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> choose(GremlinPredicate predicate,
                                                                              GremlinSteps<Seq<GremlinStep>, GremlinPredicate> trueChoice,
                                                                              GremlinSteps<Seq<GremlinStep>, GremlinPredicate> falseChoice) {
            return start().choose(predicate, trueChoice, falseChoice);
        }

        @SafeVarargs
        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> coalesce(GremlinSteps<Seq<GremlinStep>, GremlinPredicate>... traversals) {
            return start().coalesce(traversals);
        }


        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> outE() {
            return start().outE();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> path() {
            return start().path();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> properties(String... propertyKeys) {
            return start().properties(propertyKeys);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> select(String... stepLabels) {
            return start().select(stepLabels);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> start() {
            return translationBuilder().start();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> V() {
            return translationBuilder().V();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> addV() {
            return translationBuilder().addV();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> addV(String vertexLabel) {
            return translationBuilder().addV(vertexLabel);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> inject(Object... starts) {
            return translationBuilder().inject(starts);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> loops() {
            return start().loops();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> project(String... keys) {
            return start().project(keys);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> values(String... keys) {
            return start().values(keys);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> as(String key) {
            return start().as(key);
        }

        private static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> translationBuilder() {
            return Translator.builder().custom(
                new IRGremlinSteps(),
                new IRGremlinPredicates(),
                new IRGremlinBindings()
            ).build().steps();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> map(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            return start().map(traversal);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> bothE() {
            return start().bothE();
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> property(String key, GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            return start().property(key, traversal);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> property(String key, Object value) {
            return start().property(key, value);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> sideEffect(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> sideEffectTraversal) {
            return start().sideEffect(sideEffectTraversal);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> fold() {
            return start().fold();
        }


        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> by(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            return start().by(traversal);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> where(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            return start().where(traversal);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> hasLabel(String label) {
            return start().hasLabel(label);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> has(String key, GremlinPredicate predicate) {
            return start().has(key, predicate);
        }

        public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> repeat(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            return start().repeat(traversal);
        }


    }
}
