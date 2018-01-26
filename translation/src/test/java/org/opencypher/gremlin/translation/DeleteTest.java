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

import org.apache.tinkerpop.gremlin.structure.Column;
import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

public class DeleteTest {

    @Test
    public void detachDelete() {
        assertThat(parse(
            "MATCH (s:software) DETACH DELETE s"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("s")
                    .where(__.select("s").hasLabel("software"))
                    .barrier()
                    .sideEffect(
                        __.select("s")
                            .unfold()
                            .is(P.neq(Tokens.NULL))
                            .drop()
                    )
                    .barrier().limit(0)
            );
    }

    @Test
    public void detachDeleteMultiple() {
        assertThat(parse(
            "MATCH (s:software), (p:person) DETACH DELETE s, p"
        ))
            .hasTraversalBeforeReturn(
                __
                    .V().as("s")
                    .V().as("p")
                    .where(
                        __.and(
                            __.select("s").hasLabel("software"),
                            __.select("p").hasLabel("person")
                        )
                    )
                    .barrier()
                    .sideEffect(
                        __.select("s", "p")
                            .select(Column.values)
                            .unfold()
                            .is(P.neq(Tokens.NULL))
                            .drop()
                    )
                    .barrier().limit(0)
            );
    }
}
