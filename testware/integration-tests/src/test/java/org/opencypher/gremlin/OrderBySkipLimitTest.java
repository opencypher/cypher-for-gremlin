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
package org.opencypher.gremlin;

import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.TinkerGraphServerEmbedded;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OrderBySkipLimitTest {

    @ClassRule
    public static final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    private static final int VERTICES_COUNT = 6;

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.client().submitCypher(cypher);
    }

    @Test
    public void precondition() throws Exception {
        List<Map<String, Object>> results = submitAndGet("MATCH (n) RETURN n");
        assertThat(results).hasSize(VERTICES_COUNT);
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

}
