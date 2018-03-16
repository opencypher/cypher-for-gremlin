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

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class OrderByTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private static final int VERTICES_COUNT = 6;

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void orderBySingleColumn() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n.name ORDER BY n.name");
        assertThat(results).hasSize(6)
            .extracting("n.name")
            .containsExactly("josh", "lop", "marko", "peter", "ripple", "vadas");
    }

    @Test
    public void orderByMultipleColumns() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person)-[:created]->(s:software) " +
                "RETURN p.name, s.name " +
                "ORDER BY p.name ASC, s.name DESC"
        );
        assertThat(results)
            .extracting("p.name", "s.name")
            .containsExactly(
                tuple("josh", "ripple"),
                tuple("josh", "lop"),
                tuple("marko", "lop"),
                tuple("peter", "lop")
            );
    }

    @Test
    public void skip() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n SKIP 2");
        assertThat(results).hasSize(VERTICES_COUNT - 2);
    }

    @Test
    public void limit() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n LIMIT 1");
        assertThat(results).hasSize(1);
    }

    @Test
    public void orderBySkipLimit() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n.name ORDER BY n.name SKIP 2 LIMIT 1");
        assertThat(results)
            .extracting("n.name")
            .containsExactly("marko");
    }

    @Test
    public void skipOutOfSize() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n.name SKIP 20");
        assertThat(results).isEmpty();
    }

    @Test
    public void limitOutOfSize() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n.name LIMIT 10");
        assertThat(results).hasSize(VERTICES_COUNT);
    }

    @Test
    public void projections() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "WITH p, 0 AS relevance " +
                "RETURN p.age AS age " +
                "ORDER BY relevance, p.age"
        );

        System.out.println(results);
    }

    @Test
    public void orderByAfterAggregation() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person)-[:created]->(s:software) " +
                "RETURN p.name AS name, count(s) AS creations " +
                "ORDER BY creations DESC, name ASC"
        );

        assertThat(results)
            .extracting("name", "creations")
            .containsExactly(
                tuple("josh", 2L),
                tuple("marko", 1L),
                tuple("peter", 1L)
            );
    }

    @Test
    public void doubleAggregation() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person)" +
                "WITH p.name AS name, count(p) AS cnt " +
                "RETURN count(cnt) as count2"
        );

        assertThat(results)
            .extracting("count2")
            .containsExactly(4L);
    }

}
