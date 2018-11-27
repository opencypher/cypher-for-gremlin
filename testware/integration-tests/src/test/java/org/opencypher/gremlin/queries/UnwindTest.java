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

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithCosmosDB;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class UnwindTest {
    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void unwindLabels() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) UNWIND " +
                "labels(n) AS label " +
                "RETURN DISTINCT label"
        );

        assertThat(results)
            .extracting("label")
            .containsExactlyInAnyOrder("person", "software");
    }

    @Test
    public void unwindKeys() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "UNWIND keys(n) AS key " +
                "RETURN DISTINCT key"
        );

        assertThat(results)
            .extracting("key")
            .containsExactlyInAnyOrder("name", "age", "lang");
    }

    @Test
    public void injectRange() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(1, 9) AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(45L);

    }

    @Test
    public void injectRangeWithStep() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(1, 9, 2) AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(25L);
    }

    @Test
    public void injectLargeRange() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(10001, 20000) AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(150005000L);
    }

    @Test
    public void listCreate() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [1, 2, 3] AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(6L);
    }

    @Test
    @Category(SkipWithCosmosDB.TraversalInProperty.class)
    public void listCreateProperty() throws Exception {
        submitAndGet(
            "UNWIND [1, 2, 3] AS i " +
                "CREATE (n:lcp {num: i})"
        );

        List<Map<String, Object>> verification = submitAndGet(
            "MATCH (n:lcp) RETURN sum(n.num) as sum"
        );

        submitAndGet("MATCH (n:lcp) DETACH DELETE n");

        assertThat(verification)
            .extracting("sum")
            .containsExactly(6L);
    }

    @Test
    public void listReturn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [1, 2, 3] AS i " +
                "RETURN i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly(1L, 2L, 3L);
    }

    @Test
    public void listWithReturn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [1, 2, 3] AS i " +
                "WITH i AS x " +
                "RETURN x"
        );

        assertThat(results)
            .extracting("x")
            .containsExactly(1L, 2L, 3L);
    }

    @Test
    public void unwindVariable() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS numbers " +
                "UNWIND numbers AS arr " +
                "RETURN arr"
        );

        assertThat(results)
            .extracting("arr")
            .containsExactly(1L, 2L, 3L);
    }

    @Test
    public void unwindNull() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND null AS nil " +
                "RETURN nil"
        );

        assertThat(results)
            .extracting("nil")
            .isEmpty();
    }

}
