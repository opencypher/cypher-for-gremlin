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

import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

public class VariablePathTest {

    @Test
    public void anyLengthPath() {
        assertThat(parse(
            "MATCH (n)-[r*]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .repeat(__.outE().as("r").inV())
                .emit()
                .until(__.path().count(Scope.local).is(P.gte(21)))
                .as("m")
                .select("m")
        );
    }

    @Test
    public void anyLengthPathAlt() {
        assertThat(parse(
            "MATCH (n)-[r*..]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .repeat(__.outE().as("r").inV())
                .emit()
                .until(__.path().count(Scope.local).is(P.gte(21)))
                .as("m")
                .select("m")
        );
    }

    @Test
    public void fixedLengthPath() {
        assertThat(parse(
            "MATCH (n)-[r*2]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .times(2)
                .repeat(__.outE().as("r").inV())
                .as("m")
                .select("m")
        );
    }

    @Test
    public void lowerBoundedPath() {
        assertThat(parse(
            "MATCH (n)-[r*2..]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .emit()
                .repeat(__.outE().as("r").inV())
                .until(__.path().count(Scope.local).is(P.gte(21)))
                .where(__.path().count(Scope.local).is(P.gte(5)))
                .as("m")
                .select("m")
        );
    }

    @Test
    public void upperBoundedPath() {
        assertThat(parse(
            "MATCH (n)-[r*..3]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .repeat(__.outE().as("r").inV())
                .emit()
                .until(__.path().count(Scope.local).is(P.gte(7)))
                .where(__.path().count(Scope.local).is(P.lte(7)))
                .as("m")
                .select("m")
        );
    }

    @Test
    public void rangeBoundedPath() {
        assertThat(parse(
            "MATCH (n)-[r*2..3]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .emit()
                .repeat(__.outE().as("r").inV())
                .until(__.path().count(Scope.local).is(P.gte(7)))
                .where(__.path().count(Scope.local).is(P.between(5, 8)))
                .as("m")
                .select("m")
        );
    }

    @Test
    public void chainedFirstVarPath() {
        assertThat(parse(
            "MATCH (n)-[r1*2]->(x)-[r2]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .times(2)
                .repeat(__.outE().as("r1").inV())
                .as("x")
                .outE().as("r2").inV()
                .as("m")
                .select("m")
        );
    }

    @Test
    public void chainedSecondVarPath() {
        assertThat(parse(
            "MATCH (n)-[r1]->(x)-[r2*2]->(m) RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .outE().as("r1").inV()
                .as("x")
                .times(2)
                .repeat(__.outE().as("r2").inV())
                .as("m")
                .select("m")
        );
    }

    @Test
    public void varPathPredicate() throws Exception {
        assertThat(parse(
            "MATCH (n)-[r* {weight: 1.0}]->(m) RETURN n, m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .repeat(__.outE().as("r").inV())
                .emit()
                .until(__.path().count(Scope.local).is(P.gte(21)))
                .as("m")
                .where(__.select("r").values("weight").is(P.eq(1.0)))
                .select("n", "m")
        );
    }
}
