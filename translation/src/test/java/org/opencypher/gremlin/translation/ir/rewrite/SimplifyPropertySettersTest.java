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
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parameter;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.junit.Test;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class SimplifyPropertySettersTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(
            InlineMapTraversal$.MODULE$,
            SimplifyPropertySetters$.MODULE$
        ),
        seq()
    );

    @Test
    public void create() {
        assertThat(parse(
            "CREATE ({foo: 'bar', baz: null, quux: $x})"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.addV().as("  UNNAMED8")
                    .property("foo", "bar")
                    .property("quux", parameter("x"))
                    .barrier().limit(0)
            );
    }

    @Test
    public void unset() {
        assertThat(parse(
            "MATCH (n) " +
                "SET n.foo = []"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V().as("n")
                    .select("n")
                    .choose(P.neq(NULL),
                        __.sideEffect(__.properties("foo").drop()),
                        __.constant(NULL))
                    .barrier().limit(0)
            );
    }

}
