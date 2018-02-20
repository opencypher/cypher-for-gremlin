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
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class MatchTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void matchAll() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN n"
        );

        assertThat(results)
            .extracting("n")
            .hasSize(6);
    }

    @Test
    public void alias() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) RETURN p.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "vadas", "josh", "peter");
    }

    @Test
    public void matchComplex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person {name: \"marko\"})\n" +
                "MATCH (n)-[r1:created]->(lop:software {name: \"lop\"})" +
                "    <-[r2:created]-(colleague:person)\n" +
                "RETURN colleague.name"
        );

        assertThat(results)
            .extracting("colleague.name")
            .containsExactlyInAnyOrder("josh", "peter");
    }

    @Test
    public void multipleUnrelatedMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (m:person {name: \"marko\"})\n" +
                "MATCH (v:person {name: \"vadas\"})\n" +
                "RETURN m.name, v.name"
        );

        assertThat(results)
            .extracting("m.name", "v.name")
            .containsExactly(tuple("marko", "vadas"));
    }

    @Test
    public void multipleRelatedMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (m:person {name: \"marko\"})\n" +
                "MATCH (m:person {age: 29})\n" +
                "MATCH (m:person)-[r1:created]->(lop:software {name: \"lop\"})\n" +
                "RETURN m"
        );

        assertThat(results)
            .extracting("m")
            .extracting(byElementProperty("name"))
            .containsExactly("marko");
    }

    @Test
    public void multipleComplex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (j:person)-[:created]->(lop:software {name: \"ripple\"})\n" +
                "MATCH (j)<-[:knows]-(m:person)-[:created]->(s:software)\n" +
                "RETURN s, m"
        );

        assertThat(results)
            .extracting("s")
            .extracting(byElementProperty("name"))
            .containsExactly("lop");

        assertThat(results)
            .extracting("m")
            .extracting(byElementProperty("name"))
            .containsExactly("marko");
    }

    @Test
    public void returnMultipleProperties() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) RETURN n.name, n.age"
        );

        assertThat(results)
            .hasSize(4)
            .extracting("n.name", "n.age")
            .containsExactlyInAnyOrder(
                tuple("marko", 29L),
                tuple("vadas", 27L),
                tuple("josh", 32L),
                tuple("peter", 35L)
            );
    }

    @Test
    public void matchNullProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n {name: \"marko\", age: null}) RETURN n"
        );

        assertThat(results).isEmpty();
    }
}
