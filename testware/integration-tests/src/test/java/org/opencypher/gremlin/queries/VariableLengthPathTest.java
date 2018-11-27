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

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.util.Lists.newArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithCosmosDB;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.test.TestCommons.ModernGraph;
import org.opencypher.gremlin.translation.ReturnProperties;

public class VariableLengthPathTest {
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
    @Category(SkipWithCosmosDB.Truncate4096.class)
    public void variableLengthPath() throws Exception {
        String cypher = "MATCH (p:person {name: 'marko'}) " +
            "MATCH (p)-[r*1..2]->(s:software) " +
            "RETURN r, s.name AS software";
        List<Map<String, Object>> results = submitVariableLengthQuery(cypher);

        assertThat(results)
            .hasSize(3)
            .extracting("r", "software")
            .containsExactlyInAnyOrder(
                tuple(newArrayList("created"), "lop"),
                tuple(newArrayList("knows", "created"), "lop"),
                tuple(newArrayList("knows", "created"), "ripple")
            );
    }

    @Test
    public void fixedLengthPath() throws Exception {
        String cypher = "MATCH (:person {name: 'josh'})-[r*1..1]->(s:software) " +
            "RETURN r, s.name AS software";
        List<Map<String, Object>> results = submitVariableLengthQuery(cypher);

        assertThat(results)
            .hasSize(2)
            .extracting("r", "software")
            .containsExactlyInAnyOrder(
                tuple(newArrayList("created"), "lop"),
                tuple(newArrayList("created"), "ripple")
            );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> submitVariableLengthQuery(String cypher) throws ExecutionException, InterruptedException {
        return submitAndGet(cypher).stream()
            .map(result -> {
                if (result.get("r") instanceof Edge) {
                    result.put("r", newArrayList(result.get("r")));
                }

                Map<String, Object> map = new HashMap<>();
                map.put("r", ((Collection<Map>) result.get("r")).stream()
                    .map(m -> m.get(ReturnProperties.LABEL))
                    .collect(toList()));

                map.put("software", result.get("software"));
                return map;
            })
            .collect(toList());
    }

    @Test
    public void anyLengthPath() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r*]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("josh",
                "ripple",
                "lop",
                "lop",
                "vadas",
                "ripple",
                "lop",
                "lop");
    }

    @Test
    public void anyLengthPathAlt() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r*..]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("josh",
                "ripple",
                "lop",
                "lop",
                "vadas",
                "ripple",
                "lop",
                "lop");
    }


    @Test
    public void fixedLengthPath2() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r*2]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("ripple", "lop");
    }

    @Test
    public void lowerBoundedPath() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r*2..]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("ripple", "lop");
    }

    @Test
    public void upperBoundedPath() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r*..3]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("josh",
                "ripple",
                "lop",
                "lop",
                "vadas",
                "ripple",
                "lop",
                "lop");
    }

    @Test
    public void rangeBoundedPath() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r*2..3]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("ripple", "lop");
    }

    @Test
    public void chainedFirstVarPath() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r1*1]->(x)-[r2]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("ripple", "lop");
    }

    @Test
    public void chainedSecondVarPath() {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r1]->(x)-[r2*1]->(m) RETURN m.name");

        assertThat(results)
            .extracting("m.name")
            .containsExactlyInAnyOrder("ripple", "lop");
    }

    @Test
    public void varPathPredicate() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n)-[r* {weight: 1.0}]->(m) RETURN n.name, m.name");

        assertThat(results)
            .extracting("n.name", "m.name")
            .containsExactlyInAnyOrder(tuple("marko", "josh"),
                tuple("marko", "ripple"),
                tuple("josh", "ripple"));
    }

    @Test
    @Category(SkipWithCosmosDB.Truncate4096.class)
    public void multipleVarLengthRelationships() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH p = (a {name: 'marko'})-[:knows*0..1]->(b)-[:created*0..1]->(c)\n" +
                "RETURN p");

        assertThat(results)
            .extracting("p")
            .containsExactlyInAnyOrder(
                newArrayList(g.MARKO),
                newArrayList(g.MARKO, g.MARKO_CREATED_LOP, g.LOP),
                newArrayList(g.MARKO, g.MARKO_KNOWS_VADAS, g.VADAS),
                newArrayList(g.MARKO, g.MARKO_KNOWS_JOSH, g.JOSH),
                newArrayList(g.MARKO, g.MARKO_KNOWS_JOSH, g.JOSH, g.JOSH_CREATED_RIPPLE, g.RIPPLE),
                newArrayList(g.MARKO, g.MARKO_KNOWS_JOSH, g.JOSH, g.JOSH_CREATED_LOP, g.LOP)
            );
    }
}
