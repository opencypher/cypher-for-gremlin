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

import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.junit.Test;

public class RewriterTest {

    @Test
    public void propertyMatchAdjacency() {
        assertThat(parse(
            "MATCH (n:N {p: 'n'})-[r:R {p: 'r'}]->(m:M {p: 'm'}) " +
                "RETURN n, r, m"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").has("p", P.eq("n")).hasLabel("N")
                    .outE("R").as("r").has("p", P.eq("r")).inV()
                    .as("m").has("p", P.eq("m")).hasLabel("M")
                    .select("n", "r", "m")
            );
    }

    @Test
    public void propertyMatchAdjacencyWhere() {
        assertThat(parse(
            "MATCH (n)-[r:R]->(m) " +
                "WHERE (n:N) AND n.p = 'n' " +
                "AND (m:M) AND m.p = 'm' " +
                "AND r.p = 'r' " +
                "RETURN n, r, m"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").has("p", P.eq("n")).hasLabel("N")
                    .outE("R").as("r").has("p", P.eq("r")).inV()
                    .as("m").has("p", P.eq("m")).hasLabel("M")
                    .select("n", "r", "m")
            );
    }

}
