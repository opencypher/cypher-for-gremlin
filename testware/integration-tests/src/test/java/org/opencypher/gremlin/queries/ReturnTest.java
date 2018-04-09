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
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ReturnTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void single() throws Exception {
        String cypher = "MATCH (n) RETURN n";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(6);
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
        List<Map<String, Object>> maps = submitAndGet(cypher);

        List<Tuple> results = maps.stream()
            .map(result -> (List) result.get("p"))
            .map(result -> tuple(
                byElementProperty("name").extract(result.get(0)),
                byElementProperty("weight").extract(result.get(1)),
                byElementProperty("name").extract(result.get(2))
            ))
            .collect(toList());

        assertThat(results)
            .hasSize(4)
            .containsExactlyInAnyOrder(
                tuple("peter", 0.2, "lop"),
                tuple("josh", 0.4, "lop"),
                tuple("marko", 0.4, "lop"),
                tuple("josh", 1.0, "ripple")
            );
    }

    @Test
    public void returnUndirectedPath() throws Exception {
        String cypher = "MATCH p = (:person)-[:created]-(:software) RETURN p";
        List<Map<String, Object>> maps = submitAndGet(cypher);

        List<Tuple> results = maps.stream()
            .map(result -> (List) result.get("p"))
            .map(result -> tuple(
                byElementProperty("name").extract(result.get(0)),
                byElementProperty("weight").extract(result.get(1)),
                byElementProperty("name").extract(result.get(2))
            ))
            .collect(toList());

        assertThat(results)
            .hasSize(4)
            .containsExactlyInAnyOrder(
                tuple("peter", 0.2, "lop"),
                tuple("josh", 0.4, "lop"),
                tuple("marko", 0.4, "lop"),
                tuple("josh", 1.0, "ripple")
            );
    }

    @Test
    public void returnVertexAsPath() throws Exception {
        String cypher = "MATCH p = (:person) RETURN p";
        List<Object> results = submitAndGet(cypher).stream()
            .map(result -> (List) result.get("p"))
            .map(result -> byElementProperty("name").extract(result.get(0)))
            .collect(toList());

        assertThat(results)
            .hasSize(4)
            .containsExactlyInAnyOrder(
                "marko",
                "vadas",
                "peter",
                "josh"
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
            "MATCH ()-[r:created]->() RETURN count(r) AS count"
        );

        assertThat(results)
            .extracting("count")
            .containsExactly(4L);
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
    @SuppressWarnings("unchecked")
    public void nodesAndRelationshipsFunctions() throws Exception {
        String cypher = "MATCH p = (:person)-[:knows]->(:person)-[:created]->(:software)\n" +
            "RETURN nodes(p) AS nodes, relationships(p) AS rels";
        Stream<Map<String, List<Object>>> results = submitAndGet(cypher).stream()
            .map(result -> {
                Map<String, List<Object>> map = new HashMap<>();
                map.put("nodes", ((Collection<Map<String, Object>>) result.get("nodes")).stream()
                    .map(node -> byElementProperty("name").extract(node))
                    .collect(toList()));
                map.put("rels", ((Collection<Map<String, Object>>) result.get("rels")).stream()
                    .map(rel -> byElementProperty("weight").extract(rel))
                    .collect(toList()));
                return map;
            });

        assertThat(results)
            .hasSize(2)
            .extracting("nodes", "rels")
            .containsExactlyInAnyOrder(
                tuple(asList("marko", "josh", "lop"), asList(1.0, 0.4)),
                tuple(asList("marko", "josh", "ripple"), asList(1.0, 1.0))
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
}
