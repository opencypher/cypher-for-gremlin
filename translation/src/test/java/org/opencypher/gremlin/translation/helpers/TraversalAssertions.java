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

import static org.assertj.core.api.Fail.fail;

import java.util.Objects;
import java.util.function.BiConsumer;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.ir.TranslationWriter;
import org.opencypher.gremlin.translation.ir.helpers.TraversalMatcher;
import org.opencypher.gremlin.translation.ir.model.GremlinStep;
import org.opencypher.gremlin.translation.translator.Translator;
import scala.collection.Map$;
import scala.collection.Seq;

public class TraversalAssertions {

    public interface TraversalAssertion extends BiConsumer<Seq<GremlinStep>, Seq<GremlinStep>> {
    }

    public static TraversalAssertion traversalEquals = (actual, expected) -> {
        if (!Objects.equals(actual, expected)) {
            fail(
                "AST mismatch!\nExpected: <%s>\n  Actual: <%s>",
                print(expected), print(actual)
            );
        }
    };

    public static TraversalAssertion traversalContains = (actual, expected) -> {
        if (!TraversalMatcher.containsSteps(actual, expected)) {
            fail(
                "Actual traversal does not contain expected steps!\nSteps expected: <%s>\n  Actual: <%s>",
                print(expected), print(actual)
            );
        }
    };

    public static TraversalAssertion traversalNotContains = (actual, expected) -> {
        if (TraversalMatcher.containsSteps(actual, expected)) {
            fail(
                "Actual traversal expected not to contain steps!\nSteps not expected: <%s>\n  Actual: <%s>",
                print(expected), print(actual)
            );
        }
    };

    private static String print(Seq<GremlinStep> traversal) {
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();
        return TranslationWriter.write(traversal, translator, Map$.MODULE$.empty());
    }
}
