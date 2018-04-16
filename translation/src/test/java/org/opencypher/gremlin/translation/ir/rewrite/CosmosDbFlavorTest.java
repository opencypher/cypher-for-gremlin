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
package org.opencypher.gremlin.translation.ir.rewrite;

import static org.opencypher.gremlin.translation.Tokens.NULL;
import static org.opencypher.gremlin.translation.Tokens.UNUSED;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.junit.Test;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class CosmosDbFlavorTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(CosmosDbFlavor$.MODULE$),
        seq()
    );

    @Test
    public void values() {
        assertThat(parse(
            "MATCH (n:N) " +
                "RETURN n.p"
        ))
            .withFlavor(flavor)
            .hasTraversal(
                __.V().as("n").where(__.select("n").hasLabel("N")).as(UNUSED)
                    .select("n", UNUSED)
                    .map(__.project("n.p").by(
                        __.select("n").map(__.choose(
                            P.neq(NULL),
                            __.coalesce(
                                __.properties().hasKey("p").value(),
                                __.constant(NULL)),
                            __.constant(NULL)))))
            );
    }

    @Test
    public void loops() {
        assertThat(parse(
            "UNWIND range(1, 3) AS i " +
                "RETURN i"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.inject(1, 2, 3).as("i").as(UNUSED)
                    .select("i", UNUSED)
            );
    }
}
