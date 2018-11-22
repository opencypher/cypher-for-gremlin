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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithCosmosDB;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.test.TestCommons.ModernGraph;

public class OptionalMatchTest {
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
    public void allNodes() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH (n) RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder(
                "marko", "vadas", "peter", "josh",
                "lop", "ripple"
            );
    }

    @Test
    public void allRelationships() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH ()-[r]->() RETURN type(r)"
        );

        assertThat(results)
            .extracting("type(r)")
            .containsExactlyInAnyOrder(
                "created", "created", "created", "created",
                "knows", "knows"
            );
    }

    @Test
    @Category(SkipWithCosmosDB.Truncate4096.class)
    public void allPaths() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH p = ()-->() RETURN p"
        );

        assertThat(results)
            .extracting("p")
            .containsExactlyInAnyOrder(
                asList(g.MARKO, g.MARKO_KNOWS_VADAS, g.VADAS),
                asList(g.MARKO, g.MARKO_KNOWS_JOSH, g.JOSH),
                asList(g.MARKO, g.MARKO_CREATED_LOP, g.LOP),
                asList(g.JOSH, g.JOSH_CREATED_RIPPLE, g.RIPPLE),
                asList(g.JOSH, g.JOSH_CREATED_LOP, g.LOP),
                asList(g.PETER, g.PETER_CREATED_LOP, g.LOP)
            );
    }

    @Test
    public void nullProperties() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN s.name AS soft");

        assertThat(results)
            .extracting("soft")
            .containsExactlyInAnyOrder("lop", null, "ripple", "lop", "lop");
    }

    @Test
    public void nullVertices() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN s AS soft");

        assertThat(results)
            .extracting("soft")
            .containsNull();
    }

    @Test
    public void optionalMatchNotReferenced() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN p.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "vadas", "josh", "josh", "peter");
    }

    @Test
    public void nullPath() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person {name: 'vadas'}) " +
                "OPTIONAL MATCH path = (p)-[c:created]->(s:software) " +
                "RETURN path"
        );

        assertThat(results)
            .extracting("path")
            .containsExactly((Object) null);
    }

    @Test
    @Category(SkipWithCosmosDB.Truncate4096.class)
    public void allNullMatches() {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH (a:NotThere) " +
                "OPTIONAL MATCH (b:NotThere) " +
                "WITH a, b " +
                "OPTIONAL MATCH (b)-[r:NOR_THIS]->(a) " +
                "RETURN a, b, r"
        );

        assertThat(results)
            .extracting("a", "b", "r")
            .containsExactly(tuple(null, null, null));
    }

    @Test
    public void matchNullNode() {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH (a:Label) " +
                "WITH a " +
                "MATCH (a)-->(b) " +
                "RETURN b"
        );

        assertThat(results)
            .isEmpty();
    }

    @Test
    public void nullHasLabel() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person {name: 'vadas'}) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN s:software AS isSoft"
        );

        assertThat(results)
            .extracting("isSoft")
            .containsExactly((Object) null);
    }

    @Test
    public void nullCollect() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person {name: 'vadas'}) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN collect(s) AS soft"
        );

        assertThat(results)
            .hasSize(1)
            .extracting("soft")
            .containsExactly(emptyList());
    }

    @Test
    public void where() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "WITH p WHERE c IS NULL " +
                "RETURN p.name AS manager");

        assertThat(results)
            .extracting("manager")
            .containsExactly("vadas");
    }

    @Test
    public void correlated() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:person {name: 'marko'}) " +
                "OPTIONAL MATCH (a)-->(x:software) " +
                "OPTIONAL MATCH (x)-[r]->(b:person) " +
                "RETURN x.name, r"
        );

        assertThat(results)
            .extracting("x.name", "r")
            .containsExactly(tuple("lop", null));
    }
}
