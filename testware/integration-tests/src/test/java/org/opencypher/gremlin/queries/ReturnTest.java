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
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    public void ternaryLogic() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("t", true);
        args.put("t2", true);
        args.put("f", false);
        args.put("f2", false);
        args.put("n", null);
        args.put("n2", null);

        Map<String, Boolean> tests = new LinkedHashMap<>();
        tests.put("NOT $n", null);
        tests.put("NOT $t", false);
        tests.put("NOT $f", true);

        tests.put("$t AND $t2", true);
        tests.put("$t AND $f", false);
        tests.put("$f AND $f2", false);
        tests.put("$t AND $n", null);
        tests.put("$f AND $n", false);

        tests.put("$t OR $t2", true);
        tests.put("$t OR $f", true);
        tests.put("$f OR $f2", false);
        tests.put("$t OR $n", true);
        tests.put("$f OR $n", null);

        tests.put("$t XOR $t2", false);
        tests.put("$f XOR $f2", false);
        tests.put("$t XOR $f", true);
        tests.put("$f XOR $t", true);
        tests.put("$n XOR $n2", null);
        tests.put("$n XOR $t", null);
        tests.put("$t XOR $n", null);
        tests.put("$n XOR $f", null);
        tests.put("$f XOR $n", null);

        for (Map.Entry<String, Boolean> entry : tests.entrySet()) {
            String expr = entry.getKey();
            Boolean result = entry.getValue();
            List<Map<String, Object>> results = submitAndGet("RETURN " + expr, args);

            assertThat(results)
                .extracting(expr)
                .containsExactly(result);
        }
    }
}
