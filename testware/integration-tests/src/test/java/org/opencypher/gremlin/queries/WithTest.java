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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithCosmosDB;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class WithTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void rename() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (s:software) WITH s.name AS x RETURN x"
        );

        assertThat(results)
            .extracting("x")
            .containsExactlyInAnyOrder("lop", "ripple");
    }

    @Test
    public void aliasShadow() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (s:software) WITH s.name AS s RETURN s"
        );

        assertThat(results)
            .extracting("s")
            .containsExactlyInAnyOrder("lop", "ripple");
    }

    @Test
    public void where() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "WITH p WHERE p.age < 30 " +
                "RETURN p.name"
        );

        assertThat(results)
            .extracting("p.name")
            .containsExactlyInAnyOrder("marko", "vadas");
    }

    @Test
    public void withMap() throws Exception {
        assertThat(returnWith("map").toString()).isEqualTo("{name=Mats}");
        assertThat(returnWith("map.name")).isEqualTo("Mats");
        assertThat(returnWith("exists(map.name)")).isEqualTo(true);
        assertThat(returnWith("map.nonExisting")).isEqualTo(null);
        assertThat(returnWith("exists(map.nonExisting)")).isEqualTo(false);
    }

    private Object returnWith(String returnExpression) throws Exception {
        String queryTemplate = "WITH {name: 'Mats'} AS map RETURN %s AS result";
        return getResult(format(queryTemplate, returnExpression));
    }

    @Test
    public void withMapWithNullValue() throws Exception {
        String query = "WITH {notName: 0, notName2: null} AS map " +
            "RETURN exists(map.notName2) AS result";
        assertThat(getResult(query)).isEqualTo(false);
    }

    private Object getResult(String query) throws Exception {
        List<Map<String, Object>> results = submitAndGet(query);
        return results.iterator().next().get("result");
    }

    @Test
    public void skipAndLimit() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [1, 2, 3, 4, 5] AS i " +
                "WITH i SKIP 1 LIMIT 3 " +
                "RETURN i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly(2L, 3L, 4L);
    }

    @Test
    public void literals() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH 1 AS one, 'marko' AS marko, true AS yes " +
                "RETURN one, marko, yes"
        );

        assertThat(results)
            .extracting("one", "marko", "yes")
            .containsExactly(tuple(1L, "marko", true));
    }

    @Test
    public void containerIndex() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH ['Apa'] AS expr " +
                "RETURN expr[0] AS value"
        );

        assertThat(results)
            .extracting("value")
            .containsExactly("Apa");
    }

    @Test
    public void singleName() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:person) " +
                "RETURN a.name"
        );

        assertThat(results)
            .extracting("a.name")
            .containsExactlyInAnyOrder("marko", "vadas", "josh", "peter");
    }

    @Test
    public void multipleNames() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:software)<-[:created]-(s:person) " +
                "RETURN a.name, s.name"
        );

        assertThat(results)
            .extracting("a.name", "s.name")
            .containsExactlyInAnyOrder(
                tuple("lop", "marko"),
                tuple("lop", "josh"),
                tuple("lop", "peter"),
                tuple("ripple", "josh")
            );
    }

    @Test
    public void whereOrderBy() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:person) " +
                "WITH a WHERE a.age > 30 " +
                "RETURN a.name " +
                "ORDER BY a.name"
        );

        assertThat(results)
            .extracting("a.name")
            .containsExactly("josh", "peter");
    }

    @Test
    public void orderBySingleName() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:person) " +
                "RETURN a.name " +
                "ORDER BY a.name"
        );

        assertThat(results)
            .extracting("a.name")
            .containsExactly("josh", "marko", "peter", "vadas");
    }

    @Test
    public void orderByMultipleNames() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:software)<-[:created]-(s:person) " +
                "RETURN a.name, s.name " +
                "ORDER BY a.name, s.name"
        );

        assertThat(results)
            .extracting("a.name", "s.name")
            .containsExactly(
                tuple("lop", "josh"),
                tuple("lop", "marko"),
                tuple("lop", "peter"),
                tuple("ripple", "josh")
            );
    }

    @Test
    public void orderBySkipLimit() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:person) " +
                "RETURN a.name " +
                "ORDER BY a.name " +
                "SKIP 1 LIMIT 2"
        );

        assertThat(results)
            .extracting("a.name")
            .containsExactly("marko", "peter");
    }

    @Test
    public void projection() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:person)\n" +
                            "WITH a as p ORDER BY p.name LIMIT 1\n" +
                            "RETURN p.name"
        );

        assertThat(results)
            .extracting("p.name")
            .containsExactly("josh");
    }

    @Test
    @Category(SkipWithCosmosDB.InnerTraversals.class)
    public void onlyAliasesInWithAreBound() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (unbound:person {name: 'marko'})-[:created]->(bound:software {name: 'lop'})" +
                "WITH bound " +
                "MATCH (unbound)-[:created]->(bound)" +
                "RETURN unbound.name, bound.name"
        );

        assertThat(results)
            .extracting("unbound.name", "bound.name")
            .containsExactlyInAnyOrder(
                tuple("josh", "lop"),
                tuple("peter", "lop"),
                tuple("marko", "lop"));
    }
}
