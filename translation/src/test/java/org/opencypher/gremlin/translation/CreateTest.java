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
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

public class CreateTest {

    @Test
    public void createNode() {
        assertThat(parse(
            "CREATE (marko:person {name: \"marko\", age: 29})"
        ))
            .hasTraversalBeforeReturn(
                __.addV("person")
                    .as("marko")
                    .property("name", "marko")
                    .property("age", 29)
                    .barrier().limit(0)
            );
    }

    @Test
    public void createNodeWithListProperty() throws Exception {
        assertThat(parse(
            "CREATE (n:L {foo: [1, 2, 3]})"
        ))
            .hasTraversalBeforeReturn(
                __.addV("L")
                    .as("n")
                    .property("foo", asList(1, 2, 3))
                    .barrier().limit(0)
            );
    }

    @Test
    public void createEmptyNode() {
        assertThat(parse(
            "CREATE (n)"
        ))
            .hasTraversalBeforeReturn(
                __.addV().as("n")
                    .barrier().limit(0)
            );
    }

    @Test
    public void createNodesAndRelationship() {
        assertThat(parse(
            "CREATE (marko:person {name: \"marko\", age: 29}), " +
                "(vadas:person {name: \"vadas\", age: 27}), " +
                "(marko)-[r:knows {weight: 0.5}]->(vadas)"
        ))
            .hasTraversalBeforeReturn(
                __.addV("person").as("marko").property("name", "marko").property("age", 29)
                    .addV("person").as("vadas").property("name", "vadas").property("age", 27)
                    .addE("knows").from("marko").to("vadas").as("r").property("weight", 0.5)
                    .barrier().limit(0)
            );
    }

    @Test
    public void multipleCreateNodes() {
        assertThat(parse(
            "CREATE (marko:person {name: \"marko\", age: 29}) " +
                "CREATE (vadas:person {name: \"vadas\", age: 27})"
        ))
            .hasTraversalBeforeReturn(
                __.addV("person").as("marko").property("name", "marko").property("age", 29)
                    .addV("person").as("vadas").property("name", "vadas").property("age", 27)
                    .barrier().limit(0)
            );
    }

    @Test
    public void createPath() {
        assertThat(parse(
            "CREATE (n1:ln1)<-[r1:lr1]-(n2:ln2)-[r2:lr2]->(n3:ln3)"
        ))
            .hasTraversalBeforeReturn(
                __.addV("ln1").as("n1")
                    .addV("ln2").as("n2")
                    .addE("lr1").from("n2").to("n1").as("r1")
                    .addV("ln3").as("n3")
                    .addE("lr2").from("n2").to("n3").as("r2")
                    .barrier().limit(0)
            );
    }

    @Test
    public void createRelationship() {
        assertThat(parse(
            "CREATE (marko)-[r:knows {weight: 0.5}]->(vadas)"
        ))
            .hasTraversalBeforeReturn(
                __.addV().as("marko")
                    .addV().as("vadas")
                    .addE("knows").from("marko").to("vadas").as("r").property("weight", 0.5)
                    .barrier().limit(0)
            );
    }

    @Test
    public void createAndMatch() throws Exception {
        assertThat(parse(
            "CREATE (marko:person)-[r:knows]->(vadas:person) " +
                "WITH marko AS m " +
                "MATCH (m)-[r:knows]->(friend) " +
                "RETURN friend "
        ))
            .hasTraversalBeforeReturn(
                __.addV("person").as("marko")
                    .addV("person").as("vadas")
                    .addE("knows").from("marko").to("vadas").as("r")
                    .select("marko").as("m")
                    .V().as("  GENERATED1")
                    .where(__.select("  GENERATED1").where(P.isEq("m")))
                    .outE("knows").as("r").inV()
                    .as("friend")
                    .select("friend")
            );
    }

    @Test
    public void matchAndCreate() {
        assertThat(parse(
            "MATCH (marko:person),(vadas:person)\n" +
                "WHERE marko.name = 'marko' AND vadas.name = 'vadas'\n" +
                "CREATE (marko)-[r:knows]->(vadas)"
        ))
            .hasTraversalBeforeReturn(
                __
                    .V().as("marko")
                    .V().as("vadas")
                    .where(
                        __.and(
                            __.select("marko").hasLabel("person"),
                            __.select("vadas").hasLabel("person"),
                            __.select("marko").values("name").is(P.eq("marko")),
                            __.select("vadas").values("name").is(P.eq("vadas"))
                        )
                    )
                    .addE("knows").from("marko").to("vadas").as("r")
                    .barrier().limit(0)
            );
    }
}
