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
package org.opencypher.gremlin.translation.ir.verify;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.junit.Test;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class NoCustomFunctionsTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(),
        seq(NoCustomFunctions$.MODULE$)
    );

    @Test
    public void functionsAndPredicates() {
        String cypher = "MATCH (n:N) " +
            "WITH n.p AS s " +
            "WHERE s STARTS WITH 'x' " +
            "AND s ENDS WITH 'x' " +
            "AND s CONTAINS 'x' " +
            "RETURN length(s), toString(s)";
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher);
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build(flavor);

        assertThatThrownBy(() -> ast.buildTranslation(translator))
            .hasMessageContaining("contains, convertToString, endsWith, length, starsWith");
    }
}
