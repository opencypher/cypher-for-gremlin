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
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.assertj.core.groups.Tuple;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithGremlinGroovy;
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

    /**
     * During deserialization properties are lost in DetachedVertex, so {@link org.apache.tinkerpop.gremlin.structure.Element#property(java.lang.String)}
     * throws IllegalStateException
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void returnPath() throws Exception {
        String cypher = "MATCH p = (:person)-[:created]->(:software) RETURN p";
        List<Tuple> results = submitAndGet(cypher).stream()
            .map(result -> (List) result.get("p"))
            .map(result -> tuple(
                ((Element) result.get(0)).property("name").value(),
                ((Element) result.get(1)).property("weight").value(),
                ((Element) result.get(2)).property("name").value()
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

    /**
     * During deserialization properties are lost in DetachedVertex, so {@link org.apache.tinkerpop.gremlin.structure.Element#property(java.lang.String)}
     * throws IllegalStateException
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void returnVertexAsPath() throws Exception {
        String cypher = "MATCH p = (:person) RETURN p";
        List<Object> results = submitAndGet(cypher).stream()
            .map(result -> (List) result.get("p"))
            .map(result -> ((Element) result.get(0)).property("name").value())
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
                map.put("nodes", ((Collection<Vertex>) result.get("nodes")).stream()
                    .map(node -> (String) node.property("name").value())
                    .collect(toList()));
                map.put("rels", ((Collection<Edge>) result.get("rels")).stream()
                    .map(rel -> (double) rel.property("weight").value())
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
    public void literals() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN 42 AS foo, " +
                "'bar' AS bar, " +
                "true AS baz, " +
                "[1, 2, 3] AS list, " +
                "{qux: 42} AS map"
        );

        assertThat(results)
            .extracting("foo", "bar", "baz", "list", "map")
            .containsExactly(tuple(
                42L,
                "bar",
                true,
                asList(1L, 2L, 3L),
                singletonMap("qux", 42L)
            ));
    }

    @Test
    public void literalsAsList() {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN [" +
                "13, -40000," +
                "'Hello', \"World\"," +
                "true, false, TRUE, FALSE," +
                "null," +
                "[], [13, 'Hello', true, null]" +
                "] AS literals"
        );

        assertThat(results)
            .extracting("literals")
            .containsExactly(asList(
                13L, -40000L,
                "Hello", "World",
                true, false, true, false,
                null,
                emptyList(), asList(13L, "Hello", true, null)
            ));
    }
}
