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

import static org.opencypher.gremlin.translation.Tokens.UNUSED;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.junit.Test;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class RemoveUnusedAliasesTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(RemoveUnusedAliases$.MODULE$),
        seq()
    );

    @Test
    public void generated() {
        assertThat(parse(
            "MATCH (n)-->() " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .outE().inV()
                    .as(UNUSED)
                    .select("n", UNUSED)
            );
    }

    @Test
    public void explicit() {
        assertThat(parse(
            "MATCH (n)-[r]->(m) " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .outE().inV()
                    .as(UNUSED)
                    .select("n", UNUSED)
            );
    }

    @Test
    public void fromTo() {
        assertThat(parse(
            "CREATE (n)-[:R]->(m)"
        ))
            .withFlavor(flavor)
            .hasTraversal(
                __.addV().as("n")
                    .addV().as("m")
                    .addE("R").from("n").to("m")
                    .barrier().limit(0)
            );
    }

    @Test
    public void reAlias() {
        assertThat(parse(
            "MATCH (n)-->(m) " +
                "MATCH (m)-->(k) " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .outE().inV()
                    .as("m")
                    .V()
                    .as("  GENERATED1").where(__.select("  GENERATED1").where(P.eq("m")))
                    .outE().inV()
                    .as(UNUSED)
                    .select("n", UNUSED)
            );
    }

}
