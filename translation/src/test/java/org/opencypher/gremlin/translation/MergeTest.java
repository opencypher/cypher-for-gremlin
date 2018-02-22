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
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;

@SuppressWarnings("unchecked")
public class MergeTest {

    @Test
    public void mergeAnyVertex() {
        assertThat(parse(
            "MERGE (a) RETURN a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a"),
                    __.start().addV().as("a")
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeVertexByProperty() {
        assertThat(parse(
            "MERGE (a {prop: 43}) RETURN a.prop AS a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a")
                        .where(__.select("a")
                            .values("prop").is(P.eq(43))
                        ),
                    __.start().addV().as("a")
                        .property("prop", 43)
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeVertexByProperties() {
        assertThat(parse(
            "MERGE (a {prop1: 43, prop2: 'value'}) RETURN a.prop AS a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a")
                        .where(__.and(
                            __.select("a")
                                .values("prop1").is(P.eq(43)),
                            __.select("a")
                                .values("prop2").is(P.eq("value"))
                        )),
                    __.start().addV().as("a")
                        .property("prop1", 43)
                        .property("prop2", "value")
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeVertexByLabel() {
        assertThat(parse(
            "MERGE (a:Label) RETURN a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a")
                        .where(__.select("a")
                            .hasLabel("Label")
                        ),
                    __.start().addV("Label").as("a")
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeVertexByLabelAndProperties() {
        assertThat(parse(
            "MERGE (a:Label {prop1: 43, prop2: 'value'}) RETURN a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a")
                        .where(__.and(
                            __.select("a")
                                .hasLabel("Label"),
                            __.select("a")
                                .values("prop1").is(P.eq(43)),
                            __.select("a")
                                .values("prop2").is(P.eq("value"))
                        )),
                    __.start().addV("Label").as("a")
                        .property("prop1", 43)
                        .property("prop2", "value")
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeVertexAndSetPropertyOnCreate() {
        assertThat(parse(
            "MERGE (a) " +
                "ON CREATE SET a.prop = 42 " +
                "RETURN a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a"),
                    __.start().addV().as("a")
                        .choose(
                            P.neq("  cypher.null"),
                            __.start().sideEffect(
                                __.select("a").property("prop", 42)
                            )
                        )
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeVertexAndSetPropertyOnMatch() {
        assertThat(parse(
            "MERGE (a) " +
                "ON MATCH SET a.prop = 42 " +
                "RETURN a"
        )).hasTraversalBeforeReturn(
            __.inject(Tokens.START)
                .coalesce(
                    __.start().V().as("a")
                        .choose(
                            P.neq("  cypher.null"),
                            __.start().sideEffect(
                                __.select("a").property("prop", 42)
                            )),
                    __.start().addV().as("a")
                )
                .as("a")
                .select("a")
        );
    }

    @Test
    public void mergeShouldNotUseInjectStep() {
        assertThat(parse(
            "CREATE (a:X) " +
                "CREATE (b:X) " +
                "MERGE (c:X)"
        )).hasTraversalBeforeReturn(
            __
                .addV("X").as("a")
                .addV("X").as("b")
                .coalesce(
                    __.start().V().as("c").where(__.select("c")
                        .hasLabel("X")
                    ),
                    __.start().addV("X").as("c")
                ).as("c")
                .barrier().limit(0)
        );
    }

    @Test
    public void mergeRelationship() {
        assertThat(parse(
            "MATCH (a:A), (b:B) " +
                "MERGE (a)-[r:TYPE {weight: 0.1}]->(b) " +
                "RETURN r"
        )).hasTraversalBeforeReturn(
            __
                .V().as("a")
                .V().as("b")
                .where(__.and(
                    __.select("a").hasLabel("A"),
                    __.select("b").hasLabel("B"))
                )
                .coalesce(
                    __.start()
                        .V().as("  GENERATED1")
                        .where(
                            __.select("  GENERATED1").where(
                                P.eq("a")
                            )
                        )
                        .outE("TYPE").as("r")
                        .inV().as("  GENERATED2")
                        .where(
                            __.select("  GENERATED2").where(
                                P.eq("b")
                            )
                        )
                        .where(
                            __.select("r").values("weight").is(P.eq(0.1))
                        )
                        .select("a", "r", "b"),
                    __.start()
                        .addE("TYPE").from("a").to("b").as("r").property("weight", 0.1)
                        .select("a", "r", "b")
                )
                .select("r")
        );
    }

}
