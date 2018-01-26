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

import static java.util.Arrays.asList;
import static org.opencypher.gremlin.translation.Tokens.NULL;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;

public class SetTest {

    @Test
    public void updateVertexPropertyWithString() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n) SET n.name = 'marko'"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .choose(P.neq(NULL),
                    __.start().sideEffect(
                        __.select("n")
                            .property("name", "marko")
                    )
                )
                .barrier().limit(0)
        );
    }

    @Test
    public void updateVertexPropertyWithNumber() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n) SET n.age = 33"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .choose(P.neq(NULL),
                    __.start().sideEffect(
                        __.select("n")
                            .property("age", 33)
                    )
                )
                .barrier().limit(0)
        );
    }

    @Test
    public void updatePropertyWithList() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n:A) SET n.x = [1, 2, 3] RETURN n.x as x"
        )).hasTraversalBeforeReturn(
            __.V().as("n").where(
                __.select("n").hasLabel("A")
            ).choose(P.neq(NULL),
                __.start().sideEffect(
                    __.select("n").properties("x").drop()
                )
            ).choose(P.neq(NULL),
                __.start().sideEffect(
                    __.select("n")
                        .propertyList("x", asList(1, 2, 3))
                )
            ).select("n")
        );
    }

    @Test
    public void removeVertexProperty() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n:A) SET n.property1 = null RETURN n"
        )).hasTraversalBeforeReturn(
            __.V().as("n").where(
                __.select("n").hasLabel("A")
            ).choose(P.neq(NULL),
                __.start().sideEffect(
                    __.select("n").sideEffect(
                        __.properties("property1").drop()
                    )
                )
            ).select("n")
        );
    }

    @Test
    public void removeVertexProperty2() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n:A) REMOVE n.property1, n.property2 RETURN n"
        )).hasTraversalBeforeReturn(
            __.V().as("n").where(
                __.select("n").hasLabel("A")
            ).choose(P.neq(NULL),
                __.start().sideEffect(
                    __.select("n").properties("property1").drop()
                )
            ).choose(P.neq(NULL),
                __.start().sideEffect(
                    __.select("n").properties("property2").drop()
                )
            )
                .select("n")
        );
    }

    @Test
    public void setEdgeProperty() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n)-[r]->(m) SET r.property1 = 'value1' RETURN m"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .outE().as("r")
                .inV().as("m")
                .choose(P.neq(NULL),
                    __.start().sideEffect(
                        __.select("r")
                            .property("property1", "value1")
                    )
                )
                .select("m")
        );
    }

    @Test
    public void setEdgeProperty2() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n)-[r:REL]->(m) SET (r).name = 'neo4j' RETURN r"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .outE("REL").as("r")
                .inV().as("m")
                .choose(P.neq(NULL),
                    __.start().sideEffect(__.select("r")
                        .property("name", "neo4j")
                    )
                )
                .select("r")
        );
    }

    @Test
    public void unsetEdgeProperty() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n)-[r]->(m) SET r.property1 = null RETURN r"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .outE().as("r")
                .inV().as("m")
                .choose(P.neq(NULL),
                    __.start().sideEffect(
                        __.select("r").sideEffect(
                            __.properties("property1").drop()
                        )
                    )
                )
                .select("r")
        );
    }

    @Test
    public void addPropertiesWithMap() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n) SET n += {name: 'marko', age: 28} RETURN n"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .choose(P.neq(NULL),
                    __.start().sideEffect(
                        __.select("n")
                            .property("name", "marko")
                            .property("age", 28)
                    )
                )
                .select("n")
        );
    }

    @Test
    public void setPropertiesWithMap() {
        assertThat(CypherAstWrapper.parse(
            "MATCH (n) SET n = {name: 'marko', age: 28} RETURN n"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .choose(P.neq(NULL),
                    __.start()
                        .sideEffect(
                            __.select("n")
                                .properties().drop()
                        )
                )
                .choose(P.neq(NULL),
                    __.start().sideEffect(
                        __.select("n")
                            .property("name", "marko")
                            .property("age", 28)
                    )
                )
                .select("n")
        );
    }
}
