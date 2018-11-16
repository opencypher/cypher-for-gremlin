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
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.test.TestCommons.ModernGraph;

public class ReturnTest {
    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    public static ModernGraph g;

    @BeforeClass
    public static void setUp() throws Exception {
        g = TestCommons.modernGraph(gremlinServer.cypherGremlinClient());
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return submitAndGet(cypher, emptyMap());
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameters).all();
    }

    @Test
    public void single() throws Exception {
        String cypher = "MATCH (n) RETURN n";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(6);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void nestedProperty() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: {bar: 'baz'}} AS nestedMap " +
                "RETURN nestedMap.foo.bar AS nested"
        );

        assertThat(results)
            .extracting("nested")
            .containsExactly("baz");
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void propertyFromExpression() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [{bar: 'baz'}, 1] AS list " +
                "RETURN (list[0]).bar AS nested"
        );

        assertThat(results)
            .extracting("nested")
            .containsExactly("baz");
    }

    @Test
    public void collect() throws Exception {
        String cypher = "MATCH (n1) RETURN collect(n1)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .flatExtracting("collect(n1)")
            .hasSize(6);
    }

    @Test
    public void collectToMap() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person)-[:created]->(s:software) " +
                "RETURN collect({person: p.name, software: s.name}) AS creations"
        );

        assertThat(results)
            .flatExtracting("creations")
            .extracting("person", "software")
            .containsExactlyInAnyOrder(
                tuple("peter", "lop"),
                tuple("josh", "lop"),
                tuple("marko", "lop"),
                tuple("josh", "ripple")
            );
    }

    @Test
    public void distinct() throws Exception {
        String cypher = "MATCH (n1)-[r]->() RETURN DISTINCT n1.name, type(r)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(4)
            .extracting("n1.name", "type(r)")
            .containsExactlyInAnyOrder(
                tuple("marko", "created"),
                tuple("marko", "knows"),
                tuple("peter", "created"),
                tuple("josh", "created")
            );
    }

    @Test
    public void multiple() throws Exception {
        String cypher = "MATCH ()-[r]->() RETURN count(r) AS count, type(r)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(2)
            .extracting("type(r)", "count")
            .containsExactlyInAnyOrder(
                tuple("created", 4L),
                tuple("knows", 2L)
            );
    }

    @Test
    public void multipleFolded() throws Exception {
        String cypher = "MATCH (n1)-[r]->(n2) RETURN count(n1), count(n2) AS count";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .extracting("count")
            .containsExactly(6L);
    }

    @Test
    public void returnOrdering() throws Exception {
        String cypher = "MATCH (vadas {age: 27}), (marko {age: 29}), (josh {age: 32}), (peter {age: 35})\n" +
            "RETURN vadas.name, marko.name, josh.name, peter.name";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results).hasSize(1);

        Map<String, Object> first = results.get(0);
        Collection<Object> names = first.values();

        assertThat(names)
            .containsExactly("vadas", "marko", "josh", "peter");
    }

    @Test
    public void returnPath() throws Exception {
        String cypher = "MATCH p = (:person)-[:created]->(:software) RETURN p";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("p")
            .containsExactlyInAnyOrder(
                asList(g.MARKO, g.MARKO_CREATED_LOP,   g.LOP),
                asList(g.JOSH,  g.JOSH_CREATED_RIPPLE, g.RIPPLE),
                asList(g.JOSH,  g.JOSH_CREATED_LOP,    g.LOP),
                asList(g.PETER, g.PETER_CREATED_LOP,   g.LOP)
            );
    }

    @Test
    public void returnUndirectedPath() throws Exception {
        String cypher = "MATCH p = (:person)-[:created]-(:software) RETURN p";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("p")
            .containsExactlyInAnyOrder(
                asList(g. MARKO, g.MARKO_CREATED_LOP,   g.LOP),
                asList(g. JOSH,  g.JOSH_CREATED_RIPPLE, g.RIPPLE),
                asList(g. JOSH,  g.JOSH_CREATED_LOP,    g.LOP),
                asList(g. PETER, g.PETER_CREATED_LOP,   g.LOP)
            );
    }

    @Test
    public void returnVertexAsPath() throws Exception {
        String cypher = "MATCH p = (:person) RETURN p";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(4)
            .extracting("p")
            .containsExactlyInAnyOrder(
                asList(g.MARKO),
                asList(g.VADAS),
                asList(g.JOSH),
                asList(g.PETER)
            );
    }

    @Test
    public void countStar() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(results)
            .extracting("count(*)")
            .containsExactly(6L);
    }

    @Test
    public void countNodes() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN count(n) AS count"
        );

        assertThat(results)
            .extracting("count")
            .containsExactly(6L);
    }

    @Test
    public void countRelationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH ()-[r:created {weight: 0.4}]->() " +
                "RETURN count(r) AS count"
        );

        assertThat(results)
            .extracting("count")
            .containsExactly(2L);
    }

    @Test
    public void labelPredicate() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "RETURN n.name AS name, (n:software) AS software"
        );

        assertThat(results)
            .extracting("name", "software")
            .containsExactlyInAnyOrder(
                tuple("marko", false),
                tuple("vadas", false),
                tuple("josh", false),
                tuple("peter", false),
                tuple("lop", true),
                tuple("ripple", true)
            );
    }

    @Test
    public void labelPredicateCount() throws Exception {
        String cypher = "MATCH (n)\n" +
            "RETURN (n:person) AS person, count(*) AS count";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(2)
            .extracting("person", "count")
            .containsExactlyInAnyOrder(
                tuple(true, 4L),
                tuple(false, 2L)
            );
    }

    @Test
    public void keysFunction() throws Exception {
        String cypher = "MATCH (n:person) RETURN keys(n) AS keys";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(4)
            .extracting("keys")
            .allSatisfy(keys ->
                assertThat(keys)
                    .asList()
                    .hasSize(2)
                    .containsExactlyInAnyOrder("name", "age")
            );
    }

    @Test
    public void propertiesFunction() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:software) " +
                "RETURN properties(p) AS m"
        );

        assertThat(results)
            .extracting("m")
            .containsExactlyInAnyOrder(
                ImmutableMap.of("name", "ripple", "lang", "java"),
                ImmutableMap.of("name", "lop", "lang", "java")
            );
    }

    @Test
    public void propertiesFunctionOnAMap() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN properties({name: 'Popeye', level: 9001}) AS m"
        );

        assertThat(results)
            .extracting("m")
            .containsExactly(
                ImmutableMap.of("name", "Popeye", "level", 9001L)
            );
    }

    @Test
    public void distinctLabels() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN DISTINCT labels(n) AS labels"
        );

        assertThat(results)
            .extracting("labels")
            .containsExactlyInAnyOrder(
                singletonList("person"),
                singletonList("software")
            );
    }

    @Test
    public void distinctType() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH ()-[r]->() RETURN DISTINCT type(r) AS type"
        );

        assertThat(results)
            .extracting("type")
            .containsExactlyInAnyOrder("knows", "created");
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void nodesFunction() throws Exception {
        String cypher = "MATCH p = (:person)-[:knows]->(:person)\n" +
            "RETURN nodes(p) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder(
                asList(g.MARKO, g.VADAS),
                asList(g.MARKO, g.JOSH)
            );
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void nodesFunctionKeepsTraversalHistory() throws Exception {
        String cypher = "MATCH p = (first:person)-[:knows]->(:person)\n" +
            "RETURN nodes(p) as r, first.name as n";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r", "n")
            .containsExactlyInAnyOrder(
                tuple(asList(g.MARKO, g.VADAS), "marko"),
                tuple(asList(g.MARKO, g.JOSH), "marko")
            );
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    @SuppressWarnings("unchecked")
    public void nodesAndRelationshipsFunctions() throws Exception {
        String cypher = "MATCH p = (:person)-[:knows]->(:person)-[:created]->(:software)\n" +
            "RETURN nodes(p) AS nodes, relationships(p) AS rels";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("nodes", "rels")
            .containsExactlyInAnyOrder(
                tuple(asList(g.MARKO, g.JOSH, g.LOP), asList(g.MARKO_KNOWS_JOSH, g.JOSH_CREATED_LOP)),
                tuple(asList(g.MARKO, g.JOSH, g.RIPPLE), asList(g.MARKO_KNOWS_JOSH, g.JOSH_CREATED_RIPPLE))
            );
    }

    @Test
    public void returnMaxMin() throws Exception {
        String cypher = "MATCH (n:person) " +
            "RETURN max(n.age), min(n.age)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results).hasSize(1);

        Map<String, Object> first = results.get(0);
        Collection<Object> names = first.values();

        assertThat(names)
            .containsExactly(35L, 27L);
    }

    @Test
    public void returnNullMaxMin() throws Exception {
        String cypher = "MATCH (n:DoesNotExist) " +
            "RETURN max(n.age), min(n.age)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results).hasSize(1);

        Map<String, Object> first = results.get(0);
        Collection<Object> names = first.values();

        assertThat(names)
            .containsExactly(null, null);
    }

    @Test
    public void returnMaxMinWithNull() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [1, null, 3] AS i " +
                "RETURN max(i), min(i)"
        );

        assertThat(results)
            .extracting("max(i)", "min(i)")
            .containsExactly(tuple(3L, 1L));
    }

    @Test
    public void returnNull() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN null AS literal"
        );

        assertThat(results)
            .extracting("literal")
            .containsExactly((Object) null);
    }

    @Test
    public void returnCoalesce() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a) " +
                "RETURN coalesce(a.age, a.lang) AS c"
        );

        assertThat(results)
            .extracting("c")
            .containsExactlyInAnyOrder(
                27L, 29L, 32L, 35L,
                "java", "java"
            );
    }

    @Test
    public void returnRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN range(1, 5) AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void plusTest() throws Exception {
        Map<String, Object> tests = new LinkedHashMap<>();
        tests.put("1 AS a, 2 AS b", 3L);
        tests.put("'1' AS a, '2' AS b", "12");
        tests.put("1 AS a, '2' AS b", "12");
        tests.put("'1' AS a, 2 AS b", "12");
        tests.put("[1] AS a, [2] AS b", asList(1L, 2L));
        tests.put("1 AS a, [2] AS b", asList(1L, 2L));
        tests.put("[1] AS a, 2 AS b", asList(1L, 2L));
        tests.put("'1' AS a, ['2'] AS b", asList("1", "2"));
        tests.put("['1'] AS a, '2' AS b", asList("1", "2"));
        tests.put("1 AS a, null AS b", null);
        tests.put("'1' AS a, null AS b", null);
        tests.put("[1] AS a, null AS b", null);

        for (Map.Entry<String, Object> entry : tests.entrySet()) {
            String expr = entry.getKey();
            Object result = entry.getValue();
            List<Map<String, Object>> results = submitAndGet(
                "WITH " + expr + " RETURN a + b AS r"
            );

            assertThat(results)
                .extracting("r")
                .containsExactly(result);
        }
    }

    @Test
    public void optionalProjection() throws Exception {
        String cypher = "OPTIONAL MATCH (n:notExisting) WITH (n) as m RETURN m";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting("m")
            .containsExactly((Object) null);
    }

    @Test
    public void returnNestedLists() throws Exception {
        String cypher = "MATCH p=(n {name:'marko'})-[r:created]->(o)\n" +
            "WITH collect(n) as nl, collect(r) as rl, collect(p) as pl\n" +
            "WITH collect(nl) as nl2, collect(rl) as rl2, collect(pl) as pl2\n" +
            "RETURN collect(nl2) as n, collect(rl2) as r, collect(pl2) as p";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        List<Map<String, Object>> markoCreatedOssPath = asList(g.MARKO, g.MARKO_CREATED_LOP, g.LOP);

        assertThat(cypherResults)
            .extracting("n", "r", "p")
            .containsExactly(tuple(
                asList(asList(asList(g.MARKO))),
                asList(asList(asList(g.MARKO_CREATED_LOP))),
                asList(asList(asList(markoCreatedOssPath))))
            );
    }
}
