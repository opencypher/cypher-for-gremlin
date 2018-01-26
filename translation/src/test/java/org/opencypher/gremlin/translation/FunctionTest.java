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

import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;

public class FunctionTest {

    @Test
    public void countCapitalized() {
        assertThat(CypherAstFacade.parse(
            "MATCH (n) " +
                "RETURN Count(n)"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .select("n")
            );
    }

    @Test
    public void typeCapitalized() {
        assertThat(CypherAstFacade.parse(
            "MATCH (n)-[r]->(m) " +
                "WHERE Type(r) = 'foo' " +
                "RETURN n"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n").outE().as("r").inV().as("m")
                    .where(__.select("r").label().is(P.eq("foo")))
                    .select("n")
            );
    }

    @Test
    public void existsCapitalized() {
        assertThat(CypherAstFacade.parse(
            "MATCH (n) " +
                "WHERE Exists(n.name) " +
                "RETURN n"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .where(__.select("n").has("name"))
                    .select("n")
            );
    }

    @Test
    public void existsInReturn() {
        assertThat(CypherAstFacade.parse(
            "MATCH (n) " +
                "RETURN exists(n.name)"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .select("n")
        );
    }

    @Test
    public void functionsCombination() {
        assertThat(CypherAstFacade.parse(
            "MATCH (n) " +
                "RETURN sum(size(keys(n))) AS totalNumberOfProps"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .select("n")
        );
    }

    @Test
    public void rangeCapitalized() {
        assertThat(CypherAstFacade.parse(
            "UNWIND Range(1, 3) AS r " +
                "RETURN r"
        ))
            .hasTraversalBeforeReturn(
                __.inject(1, 2, 3).as("r")
                    .select("r")
            );
    }

}
