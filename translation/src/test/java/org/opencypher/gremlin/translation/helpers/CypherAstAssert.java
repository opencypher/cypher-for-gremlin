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

import static java.util.function.Function.identity;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;
import static org.opencypher.gremlin.translation.helpers.TraversalAssertions.traversalContains;
import static org.opencypher.gremlin.translation.helpers.TraversalAssertions.traversalNotContains;

import java.util.Objects;
import java.util.function.Function;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Preconditions;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.helpers.TraversalAssertions.TraversalAssertion;
import org.opencypher.gremlin.translation.ir.builder.IRGremlinBindings;
import org.opencypher.gremlin.translation.ir.builder.IRGremlinPredicates;
import org.opencypher.gremlin.translation.ir.builder.IRGremlinSteps;
import org.opencypher.gremlin.translation.ir.model.GremlinPredicate;
import org.opencypher.gremlin.translation.ir.model.GremlinStep;
import org.opencypher.gremlin.translation.ir.rewrite.GremlinRewriter;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;
import scala.collection.Seq;

public class CypherAstAssert extends AbstractAssert<CypherAstAssert, CypherAstWrapper> {

    private final CypherAstWrapper actual;
    private TranslatorFlavor flavor;
    private GremlinRewriter rewriter;
    private Function<Seq<GremlinStep>, Seq<GremlinStep>> extractor = identity();

    CypherAstAssert(CypherAstWrapper actual) {
        super(actual, CypherAstAssert.class);
        this.actual = actual;
    }

    public CypherAstAssert withFlavor(TranslatorFlavor flavor) {
        this.flavor = flavor;
        return this;
    }

    public CypherAstAssert rewritingWith(GremlinRewriter rewriter) {
        this.rewriter = rewriter;
        return this;
    }

    public CypherAstAssert normalizedTo(String expected) {
        String actualString = actual.toString();
        String expectedString = CypherAstWrapper.parse(expected).toString();
        if (!Objects.equals(actualString, expectedString)) {
            failWithMessage(
                "AST mismatch!\nExpected: <%s>\n  Actual: <%s>",
                expectedString, actualString
            );
        }
        return this;
    }

    public final CypherAstAssert adds(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
        assertTraversal(traversal, traversalNotContains);
        assertTraversal(rewriteTraversal(), traversal, traversalContains);
        return this;
    }

    public final CypherAstAssert removes(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            assertTraversal(traversal, traversalContains);
            assertTraversal(rewriteTraversal(), traversal, traversalNotContains);
        return this;
    }

    public final CypherAstAssert keeps(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> traversal) {
            assertTraversal(traversal, traversalContains);
            assertTraversal(rewriteTraversal(), traversal, traversalContains);
        return this;
    }

    private CypherAstAssert assertTraversal(Seq<GremlinStep> actual,
                                            GremlinSteps<Seq<GremlinStep>, GremlinPredicate> expected,
                                            TraversalAssertion assertion) {
        assertion.accept(extractor.apply(actual), expected.current());
        return this;
    }

    private CypherAstAssert assertTraversal(GremlinSteps<Seq<GremlinStep>, GremlinPredicate> expected, TraversalAssertion assertion) {
        assertion.accept(actualTraversal(), expected.current());
        return this;
    }

    private Seq<GremlinStep> actualTraversal() {
        Seq<GremlinStep> steps = actual.buildTranslation(irTranslator(flavor));
        steps = extractor.apply(steps);
        return steps;
    }

    private Seq<GremlinStep> rewriteTraversal() {
        Preconditions.checkNotNull(rewriter, "Rewriter not set! Use `CypherAstAssert.rewritingWith`");
        Seq<GremlinStep> steps = actual.buildTranslation(irTranslator(flavor.extend(seq(rewriter), seq())));
        return steps;
    }

    private Translator<Seq<GremlinStep>, GremlinPredicate> irTranslator(TranslatorFlavor flavor) {
        return Translator
            .builder()
            .custom(
                new IRGremlinSteps(),
                new IRGremlinPredicates(),
                new IRGremlinBindings()
            )
            .build(flavor);
    }

    public static GremlinSteps<Seq<GremlinStep>, GremlinPredicate> __() {
        return new IRGremlinSteps();
    }

    public static final IRGremlinPredicates P = new IRGremlinPredicates();
}
