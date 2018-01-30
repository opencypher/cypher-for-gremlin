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

import org.assertj.core.api.AbstractAssert;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.TranslationBuilder;
import org.opencypher.gremlin.translation.Translator;
import org.opencypher.gremlin.translation.TranslatorFactory;
import org.opencypher.gremlin.translation.string.StringPredicate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.function.Function.identity;
import static org.opencypher.gremlin.translation.Tokens.AGGREGATION;
import static org.opencypher.gremlin.translation.Tokens.PIVOT;

public class CypherAstAssert extends AbstractAssert<CypherAstAssert, CypherAstWrapper> {

    private final String actualTranslation;

    CypherAstAssert(CypherAstWrapper actual) {
        super(actual, CypherAstAssert.class);
        Translator<String, StringPredicate> dsl = TranslatorFactory.string();
        actualTranslation = actual.buildTranslation(dsl);
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

    public CypherAstAssert hasTraversal(TranslationBuilder traversal) {
        return hasTraversal(traversal, identity());
    }

    /**
     * This pattern matches the Gremlin translation of a {@code RETURN} clause.
     * It might need to be modified if {@code RETURN} translation changes.
     *
     * @see org.opencypher.gremlin.translation.walker.ReturnWalker
     * @see #hasTraversalBeforeReturn(TranslationBuilder)
     */
    private static final Pattern RETURN_START = Pattern.compile(
        "\\.(" +
            "group\\(\\)\\.by\\([^)]+\\)\\.by\\([^)]+\\)|" +
            "project\\('" + PIVOT + "'\\)|" +
            "fold\\(\\)\\.project\\('" + AGGREGATION + "'\\)" +
            ").*$"
    );

    public CypherAstAssert hasTraversalBeforeReturn(TranslationBuilder traversal) {
        // Extract everything up to the start of the RETURN clause translation
        return hasTraversal(traversal, t -> RETURN_START.matcher(t).replaceFirst(""));
    }

    private CypherAstAssert hasTraversal(TranslationBuilder traversal,
                                         Function<String, String> extractor) {
        isNotNull();

        String actual = Optional.ofNullable(actualTranslation)
            .map(extractor)
            .orElse(null);
        String expected = Optional.ofNullable(traversal)
            .map(Object::toString)
            .orElse(null);
        if (!Objects.equals(actual, expected)) {
            failWithMessage(
                "AST mismatch for traversal!\nExpected: <%s>\n  Actual: <%s>",
                expected, actual
            );
        }

        return this;
    }
}
