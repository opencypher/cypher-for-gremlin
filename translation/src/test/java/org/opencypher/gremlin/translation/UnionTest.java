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

import static org.opencypher.gremlin.translation.Tokens.START;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

@SuppressWarnings("unchecked")
public class UnionTest {
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
                        __.coalesce(
                            __.project("name").
                                by(__.constant("john"))),
                        __.coalesce(
                            __.project("name").
                                by(__.constant("jane"))),
                        __.coalesce(
                            __.project("name").
                                by(__.constant("john"))))
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
                            coalesce(
                                __.project("lang").
                                    by(__.constant("clojure")))
                        , __.is(P.neq(START)).
                            inject("java", "scala").
                            as("lang").
                            select("lang").
                            coalesce(
                                __.project("lang").
                                    by(__.identity()))
                        , __.start().V().
                            as("s").
                            where(
                                __.select("s").
                                    hasLabel("software")).
                            select("s").
                            coalesce(
                                __.project("lang").
                                    by(__.choose(P.neq("  cypher.null"), __.coalesce(__.values("lang"), __.constant("  cypher.null")), __.constant("  cypher.null"))))
                    ).
                    dedup()
            );
    }
}
