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
package org.opencypher.gremlin.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;

import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithJanusGraph;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.test.TestCommons.ModernGraph;

public class MatchTest {
    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    public static ModernGraph g;

    @BeforeClass
    public static void setUp() throws Exception {
        g = TestCommons.modernGraph(gremlinServer.cypherGremlinClient());
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void matchAll() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN n"
        );

        assertThat(results)
            .extracting("n")
            .containsExactlyInAnyOrder(g.MARKO, g.VADAS, g.JOSH, g.PETER, g.LOP, g.RIPPLE);
    }

    @Test
    public void alias() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) RETURN p.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "vadas", "josh", "peter");
    }

    @Test
    public void relationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)-[r {weight: 0.4}]->(m) " +
                "RETURN n.name, type(r), m.name"
        );

        assertThat(results)
            .extracting("n.name", "type(r)", "m.name")
            .containsExactlyInAnyOrder(
                tuple("marko", "created", "lop"),
                tuple("josh", "created", "lop")
            );
    }

    @Test
    public void undirectedRelationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)-[r {weight: 0.4}]-(m) " +
                "RETURN n.name, m.name"
        );

        assertThat(results)
            .extracting("n.name", "m.name")
            .containsExactlyInAnyOrder(
                tuple("marko", "lop"),
                tuple("josh", "lop"),
                tuple("lop", "marko"),
                tuple("lop", "josh")
            );
    }

    @Test
    public void matchComplex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person {name: \"marko\"})\n" +
                "MATCH (n)-[r1:created]->(lop:software {name: \"lop\"})" +
                "    <-[r2:created]-(colleague:person)\n" +
                "RETURN colleague.name"
        );

        assertThat(results)
            .extracting("colleague.name")
            .containsExactlyInAnyOrder("josh", "peter");
    }

    @Test
    public void multipleUnrelated() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (m:person {name: \"marko\"})\n" +
                "MATCH (v:person {name: \"vadas\"})\n" +
                "RETURN m.name, v.name"
        );

        assertThat(results)
            .extracting("m.name", "v.name")
            .containsExactly(tuple("marko", "vadas"));
    }

    @Test
    public void unrelatedRelationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "MATCH (o)<-[k:knows]-(m:person) " +
                "RETURN p.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .hasSize(8)
            .containsExactlyInAnyOrder(
                "marko", "vadas", "peter", "josh",
                "marko", "vadas", "peter", "josh"
            );
    }

    @Test
    public void unrelatedAndRelatedRelationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "MATCH (s:software) " +
                "MATCH (p)<-[k:knows]-(m:person)" +
                "RETURN p.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .hasSize(4)
            .containsExactlyInAnyOrder(
                "vadas", "josh",
                "vadas", "josh"
            );
    }

    @Test
    public void multipleRelated() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (m:person {name: \"marko\"})\n" +
                "MATCH (m:person {age: 29})\n" +
                "MATCH (m:person)-[r1:created]->(lop:software {name: \"lop\"})\n" +
                "RETURN m"
        );

        assertThat(results)
            .extracting("m")
            .containsExactly(g.MARKO);
    }

    @Test
    public void nodesSeparatelyFromRelationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a {name: 'marko'}), (b {name: 'vadas'}) " +
                "MATCH (a)-[r]->(b) " +
                "RETURN type(r) AS rel"
        );

        assertThat(results)
            .extracting("rel")
            .containsExactly("knows");
    }

    @Test
    public void multipleComplex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (j:person)-[:created]->(lop:software {name: \"ripple\"})\n" +
                "MATCH (j)<-[:knows]-(m:person)-[:created]->(s:software)\n" +
                "RETURN s, m"
        );

        assertThat(results)
            .extracting("s")
            .extracting(byElementProperty("name"))
            .containsExactly("lop");

        assertThat(results)
            .extracting("m")
            .extracting(byElementProperty("name"))
            .containsExactly("marko");
    }

    @Test
    public void returnMultipleProperties() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) RETURN n.name, n.age"
        );

        assertThat(results)
            .hasSize(4)
            .extracting("n.name", "n.age")
            .containsExactlyInAnyOrder(
                tuple("marko", 29L),
                tuple("vadas", 27L),
                tuple("josh", 32L),
                tuple("peter", 35L)
            );
    }

    @Test
    @Category(SkipWithJanusGraph.ChangePropertyType.class)
    public void emptyMatchNullProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n {name: \"marko\", age: null}) RETURN n"
        );

        assertThat(results).isEmpty();
    }

    @Test
    public void undirectedUnnamedRelationship() {
        String cypher = "MATCH (i) WHERE size( (i)--() ) > 2 RETURN i.name";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("i.name")
            .containsExactlyInAnyOrder("marko", "lop", "josh");
    }
}
