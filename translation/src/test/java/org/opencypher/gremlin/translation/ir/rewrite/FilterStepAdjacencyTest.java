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

import static org.opencypher.gremlin.translation.Tokens.START;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.junit.Test;
import org.opencypher.gremlin.translation.Tokens;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class FilterStepAdjacencyTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(FilterStepAdjacency$.MODULE$),
        seq()
    );

    @Test
    public void singlePattern() {
        assertThat(parse(
            "MATCH (n:N) " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").hasLabel("N")
                    .select("n")
            );
    }

    @Test
    public void singleWhere() {
        assertThat(parse(
            "MATCH (n) " +
                "WHERE n.p = 'n' " +
                "AND 1 <> 2 " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").has("p", P.eq("n"))
                    .where(__.constant(1).is(P.neq(2)))
                    .select("n")
            );
    }

    @Test
    public void multiplePatterns() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r:R {p: 'r'}]->(m:M {p: 'm'}) " +
                "WHERE 1 <> 2 " +
                "RETURN n, r, m"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").hasLabel("N").has("p", P.eq("n"))
                    .outE("R").as("r").has("p", P.eq("r")).inV()
                    .as("m").hasLabel("M").has("p", P.eq("m"))
                    .where(__.constant(1).is(P.neq(2)))
                    .select("n", "r", "m")
            );
    }

    @Test
    public void multipleWhere() {
        assertThat(parse(
            "MATCH (n)-[r:R]->(m) " +
                "WHERE (n:N) AND n.p = 'n' " +
                "AND (m:M) AND m.p = 'm' " +
                "AND r.p = 'r' " +
                "RETURN n, r, m"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").hasLabel("N").has("p", P.eq("n"))
                    .outE("R").as("r").has("p", P.eq("r")).inV()
                    .as("m").hasLabel("M").has("p", P.eq("m"))
                    .select("n", "r", "m")
            );
    }

    @Test
    public void multiplePaths() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r1:R]->(m:M {p: 'm'})<-[r2:R]-(k)" +
                "MATCH (k:K {p: 'k'}) " +
                "RETURN k"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").hasLabel("N").has("p", P.eq("n"))
                    .outE("R").as("r1").inV()
                    .as("m").hasLabel("M").has("p", P.eq("m"))
                    .inE("R").as("r2").outV()
                    .as("k").hasLabel("K").has("p", P.eq("k"))
                    .where(
                        __.select("r2").as(Tokens.TEMP)
                            .select("r1").where(P.neq(Tokens.TEMP))
                    )
                    .V().as("  GENERATED1").where(__.select("  GENERATED1").where(P.eq("k")))
                    .select("k")
            );
    }

    @Test
    public void variablePath() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r*1..2]->(m) " +
                "RETURN m"
        ))
            .withFlavor(flavor)
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").hasLabel("N").has("p", P.eq("n"))
                    .emit().repeat(__.outE().as("r").inV())
                    .until(__.path().count(Scope.local).is(P.gte(5)))
                    .where(__.path().count(Scope.local).is(P.between(3, 6)))
                    .as("m")
                    .select("m")
            );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void merge() {
        assertThat(parse(
            "MERGE (n:N {p: 'n'})"
        ))
            .withFlavor(flavor)
            .hasTraversal(
                __.inject(START).coalesce(
                    __.start().V().as("n").hasLabel("N").has("p", P.eq("n")),
                    __.start().addV("N").as("n").property("p", "n")
                ).as("n").barrier().limit(0)
            );
    }

}
