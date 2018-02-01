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
package org.opencypher.gremlin.translation;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

import static org.opencypher.gremlin.translation.Tokens.PIVOT;
import static org.opencypher.gremlin.translation.Tokens.START;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

@SuppressWarnings("unchecked")
public class UnionTest {

    @Test
    public void union() throws Exception {
        assertThat(parse(
            "RETURN 42 AS foo, 'bar' AS bar " +
                "UNION " +
                "MATCH (n) RETURN n AS foo, n.name AS bar " +
                "UNION " +
                "MATCH (n) RETURN count(n) AS foo, labels(n) AS bar"
        )).hasTraversal(
            __.inject(START).
                union(
                    __.project(PIVOT).
                        by(
                            __.project("foo", "bar").
                                by(__.constant(42)).
                                by(__.constant("bar"))).
                        unfold(), __.start().V().
                        as("n").
                        select("n").
                        project(PIVOT).
                        by(
                            __.project("foo", "bar").
                                by(__.as("n_1")).
                                by(__.choose(P.neq("  cypher.null"), __.coalesce(__.values("name"), __.constant("  cypher.null")), __.constant("  cypher.null")))).
                        unfold(), __.start().V().
                        as("n").
                        where(
                            __.select("n").
                                where(P.eq("n"))).
                        select("n").
                        group().
                        by(
                            __.project("bar").
                                by(
                                    __.as("n_1").
                                        label().
                                        is(P.neq(Vertex.DEFAULT_LABEL)).
                                        fold())).
                        by(
                            __.fold().
                                project("foo").
                                by(
                                    __.unfold().
                                        as("n_1").
                                        is(P.neq("  cypher.null")).
                                        count())).
                        unfold()).
                dedup()
        );
    }

    @Test
    public void simpleUnionAll() throws Exception {
        assertThat(parse(
            "RETURN 'john' AS name " +
                "UNION ALL " +
                "RETURN 'jane' AS name " +
                "UNION ALL " +
                "RETURN 'john' AS name"
        ))
            .hasTraversal(
                __.inject(START).
                    union(
                        __.project(PIVOT).
                            by(
                                __.project("name").
                                    by(__.constant("john"))).
                            unfold(),
                        __.project(PIVOT).
                            by(
                                __.project("name").
                                    by(__.constant("jane"))).
                            unfold(),
                        __.project(PIVOT).
                            by(
                                __.project("name").
                                    by(__.constant("john"))).
                            unfold())
            );
    }

    @Test
    public void unionDifferentClauses() throws Exception {
        assertThat(parse(
            "RETURN 'clojure' AS lang " +
                "UNION " +
                "UNWIND ['java', 'scala'] AS lang RETURN lang " +
                "UNION " +
                "MATCH (s:software) RETURN s.lang AS lang"
        ))
            .hasTraversal(
                __.inject(START).
                    union(
                        __.
                            project(PIVOT).
                            by(
                                __.project("lang").
                                    by(__.constant("clojure"))).
                            unfold(), __.is(P.neq(START)).
                            inject("java", "scala").
                            as("lang").
                            select("lang").
                            project(PIVOT).
                            by(
                                __.project("lang").
                                    by(__.as("lang_1"))).
                            unfold(), __.start().V().
                            as("s").
                            where(
                                __.select("s").
                                    hasLabel("software")).
                            select("s").
                            project(PIVOT).
                            by(
                                __.project("lang").
                                    by(__.choose(P.neq("  cypher.null"), __.coalesce(__.values("lang"), __.constant("  cypher.null")), __.constant("  cypher.null")))).
                            unfold()).
                    dedup()
            );
    }
}
