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

import static org.opencypher.gremlin.translation.CypherAstWrapper.parse;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssert.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssert.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.junit.Test;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class GroupStepFiltersTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(
            InlineMapTraversal$.MODULE$
        ),
        seq()
    );

    @Test
    public void singlePattern() {
        assertThat(parse(
            "MATCH (n:N) " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(
                __().where(__().select("n").hasLabel("N")))
            .keeps(
                __().hasLabel("N"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void singleWhere() {
        assertThat(parse(
            "MATCH (n) " +
                "WHERE n.p = 'n' " +
                "AND 1 <> 2 " +
                "RETURN n"
        ))
            .withFlavor(flavor)
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(
                __().where(__().and(
                    __().select("n").values("p").is(P.isEq("n")),
                    __().constant(1).is(P.neq(2))
                )))
            .adds(
                __().has("p", P.isEq("n"))
                    .where(__().constant(1).is(P.neq(2))));
    }

    @Test
    public void multiplePatterns() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r:R {p: 'r'}]->(m:M {p: 'm'}) " +
                "WHERE 1 <> 2 " +
                "RETURN n, r, m"
        ))
            .withFlavor(flavor)
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(__().select("n").values("p").is(P.isEq("n")))
            .removes(__().select("r").values("p").is(P.isEq("r")))
            .removes(__().select("m").values("p").is(P.isEq("m")))
            .adds(__().hasLabel("N").has("p", P.isEq("n")))
            .adds(__().as("r").has("p", P.isEq("r")))
            .adds(__().hasLabel("M").has("p", P.isEq("m")));
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
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(__().select("n").values("p").is(P.isEq("n")))
            .removes(__().select("r").values("p").is(P.isEq("r")))
            .removes(__().select("m").values("p").is(P.isEq("m")))
            .adds(__().hasLabel("N").has("p", P.isEq("n")))
            .adds(__().as("r").has("p", P.isEq("r")))
            .adds(__().hasLabel("M").has("p", P.isEq("m")));
    }

    @Test
    public void multiplePaths() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r1:R]->(m:M {p: 'm'})<-[r2:R]-(k)" +
                "MATCH (k:K {p: 'k'}) " +
                "RETURN k"
        ))
            .withFlavor(flavor)
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(__().select("n").values("p").is(P.isEq("n")))
            .removes(__().select("k").values("p").is(P.isEq("k")))
            .removes(__().select("m").values("p").is(P.isEq("m")))
            .adds(__().as("n").hasLabel("N").has("p", P.isEq("n")))
            .adds(__().as("m").hasLabel("M").has("p", P.isEq("m")))
            .adds(__().as("k").hasLabel("K").has("p", P.isEq("k")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void variablePath() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r*1..2]->(m) " +
                "RETURN m"
        ))
            .withFlavor(flavor)
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(
                __().where(__().and(
                    __().select("n").values("p").is(P.isEq("n")),
                    __().select("n").hasLabel("N"))))
            .adds(
                __().as("n").hasLabel("N").has("p", P.isEq("n")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void merge() {
        assertThat(parse(
            "MERGE (n:N {p: 'n'})"
        ))
            .withFlavor(flavor)
            .rewritingWith(GroupStepFilters$.MODULE$)
            .removes(
                __().where(__().and(
                    __().select("n").hasLabel("N"),
                    __().select("n").values("p").is(P.isEq("n")))))
            .adds(
                __().as("n").hasLabel("N").has("p", P.isEq("n")));

    }

}
