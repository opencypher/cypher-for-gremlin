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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class OptionalMatchTest {

    @ClassRule
    public static final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher);
    }

    @Test
    public void nullProperties() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "OPTIONAL MATCH (p)-[c:created]->(s:software) " +
                "RETURN s.name AS soft");

        assertThat(results)
            .extracting("soft")
            .containsExactly("lop", null, "ripple", "lop", "lop");
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
}
