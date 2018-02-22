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

import static org.opencypher.gremlin.translation.Tokens.NULL;
import static org.opencypher.gremlin.translation.Tokens.START;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;

import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;

@SuppressWarnings("unchecked")
public class OptionalMatchTest {

    @Test
    public void singleNode() {
        assertThat(CypherAstWrapper.parse(
            "OPTIONAL MATCH (n) RETURN n"
        ))
            .hasTraversalBeforeReturn(
                __.inject(START)
                    .coalesce(
                        __.start().V().as("n"),
                        __.constant(NULL)
                    )
                    .as("n")
                    .select("n")
            );
    }

    @Test
    public void multipleNodes() throws Exception {
        assertThat(CypherAstWrapper.parse(
            "OPTIONAL MATCH (n)-[r]->(m) RETURN n"
        ))
            .hasTraversalBeforeReturn(
                __.inject(START)
                    .coalesce(
                        __.start().V().as("n").outE().as("r").inV().as("m").select("n", "r", "m"),
                        __.constant(NULL).as("n").as("r").as("m").select("n", "r", "m")
                    )
                    .select("n")
            );
    }

    @Test
    public void path() throws Exception {
        assertThat(CypherAstWrapper.parse(
            "OPTIONAL MATCH p = (n)-[r]->(m) RETURN p"
        ))
            .hasTraversalBeforeReturn(
                __.inject(START)
                    .coalesce(
                        __.start().V().as("n").outE().as("r").inV().as("m").path().as("p").select("n", "r", "m", "p"),
                        __.constant(NULL).as("n").as("r").as("m").as("p").select("n", "r", "m", "p")
                    )
                    .select("p")
            );
    }

    @Test
    public void mixMatch() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (p:person) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN p.name AS name"
        ))
            .hasTraversalBeforeReturn(
                __.V().as("p")
                    .where(__.select("p").hasLabel("person"))
                    .coalesce(
                        __.start().V().as("  GENERATED1")
                            .where(__.select("  GENERATED1").where(P.eq("p")))
                            .outE("created").as("c").inV().as("s")
                            .where(__.select("s").hasLabel("software"))
                            .select("p", "c", "s"),
                        __.constant(NULL)
                            .as("  GENERATED2").as("c").as("s")
                            .select("  GENERATED2", "c", "s")
                    )
                    .select("p")
            );
    }
}
