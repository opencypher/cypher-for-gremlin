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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesCollectionsInProperties;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class MergeTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Before
    public void setUp() {
        submitAndGet(DELETE_ALL);
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void anyVertex() {
        List<Map<String, Object>> merge1 = submitAndGet("MERGE (n) RETURN n");
        List<Map<String, Object>> merge2 = submitAndGet("MERGE (n) RETURN n");
        List<Map<String, Object>> match = submitAndGet("MATCH (n) RETURN n");

        assertThat(merge1).hasSize(1);
        assertThat(merge2).hasSize(1);
        assertThat(match).hasSize(1);
    }

    @Test
    public void anyVertexMultiple() {
        submitAndGet("CREATE (n)");
        submitAndGet("CREATE (n)");
        List<Map<String, Object>> merge1 = submitAndGet("MERGE (n) RETURN n");
        List<Map<String, Object>> merge2 = submitAndGet("MERGE (n) RETURN n");
        List<Map<String, Object>> match = submitAndGet("MATCH (n) RETURN n");

        assertThat(merge1).hasSize(2);
        assertThat(merge2).hasSize(2);
        assertThat(match).hasSize(2);
    }

    @Test
    public void byProperty() {
        submitAndGet("MERGE (n {baz: 1})");
        submitAndGet("MERGE (n {baz: 1})");
        submitAndGet("MERGE (n {baz: 2})");
        List<Map<String, Object>> match = submitAndGet(
            "MATCH (n) RETURN n.baz"
        );

        assertThat(match)
            .extracting("n.baz")
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void byMultipleProperties() {
        submitAndGet("MERGE (n {baz: 1, bar: 1})");
        submitAndGet("MERGE (n {baz: 1, bar: 1})");
        submitAndGet("MERGE (n {baz: 1})");
        submitAndGet("MERGE (n {bar: 1})");
        submitAndGet("MERGE (n {baz: 1, bar: 2})");
        submitAndGet("MERGE (n {baz: 2, bar: 1})");
        List<Map<String, Object>> match = submitAndGet(
            "MATCH (n) RETURN n.baz, n.bar"
        );

        assertThat(match)
            .extracting("n.baz", "n.bar")
            .containsExactlyInAnyOrder(
                tuple(1L, 1L),
                tuple(1L, 2L),
                tuple(2L, 1L)
            );
    }

    @Test
    public void byLabel() {
        submitAndGet("MERGE (n:Foo)");
        submitAndGet("MERGE (n:Foo)");
        submitAndGet("MERGE (n:Bar)");
        List<Map<String, Object>> match = submitAndGet(
            "MATCH (n) RETURN labels(n) AS labels"
        );

        assertThat(match)
            .extracting("labels")
            .containsExactlyInAnyOrder(
                singletonList("Foo"),
                singletonList("Bar")
            );
    }

    @Test
    public void byLabelAndProperties() {
        submitAndGet("MERGE (n:Foo {baz: 1, bar: 1})");
        submitAndGet("MERGE (n:Foo {baz: 1, bar: 1})");
        submitAndGet("MERGE (n:Bar {baz: 1, bar: 1})");
        submitAndGet("MERGE (n:Foo)");
        submitAndGet("MERGE (n:Bar)");
        submitAndGet("MERGE (n {baz: 1})");
        submitAndGet("MERGE (n {bar: 1})");
        submitAndGet("MERGE (n:Foo {baz: 1, bar: 2})");
        submitAndGet("MERGE (n:Bar {baz: 2, bar: 1})");
        List<Map<String, Object>> match = submitAndGet(
            "MATCH (n) RETURN labels(n), n.baz, n.bar"
        );

        assertThat(match)
            .extracting("labels(n)", "n.baz", "n.bar")
            .containsExactlyInAnyOrder(
                tuple(singletonList("Foo"), 1L, 1L),
                tuple(singletonList("Bar"), 1L, 1L),
                tuple(singletonList("Foo"), 1L, 2L),
                tuple(singletonList("Bar"), 2L, 1L)
            );
    }

    @Test
    public void byRelationship() {
        submitAndGet(
            "CREATE (a:A), (b:B) " +
                "CREATE (a)-[:TYPE {name: 'r1'}]->(b) " +
                "CREATE (a)-[:TYPE {name: 'r2'}]->(b)"
        );

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a:A), (b:B) " +
                "MERGE (a)-[r:TYPE {name: 'r2'}]->(b) " +
                "RETURN count(r) AS count"
        );

        assertThat(results)
            .extracting("count")
            .containsExactly(1L);
    }

    @Test
    @Category(UsesCollectionsInProperties.ListDataType.class)
    public void byRelationshipListProperty() {
        submitAndGet(
            "CREATE (a:A), (b:B) " +
                "CREATE (a)-[:T {prop: [42, 43]}]->(b)"
        );
        submitAndGet(
            "MATCH (a:A), (b:B) " +
                "MERGE (a)-[r:T {prop: [42, 43]}]->(b)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)-[r:T {prop: [42, 43]}]->(b) " +
                "RETURN count(*) AS count"
        );

        assertThat(results)
            .extracting("count")
            .containsExactly(1L);
    }

    @Test
    public void mergeImmediatelyAfterCreate() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (a:X) " +
                "CREATE (b:X) " +
                "MERGE (c:X) " +
                "RETURN c"
        );

        assertThat(results)
            .hasSize(2);
    }

    @Test
    public void vertexOn() throws Exception {
        String query = "MERGE (a:lbl {prop3: 'value'}) " +
            "ON MATCH SET a.action = 'on match' " +
            "ON CREATE SET a.action = 'on create' " +
            "RETURN a.action, a.prop3, labels(a)";

        // checking created vertex properties
        List<Map<String, Object>> results = submitAndGet(query);
        assertThat(results)
            .extracting("a.prop3", "labels(a)")
            .containsExactly(tuple("value", singletonList("lbl")));

        // checking SET clause
        assertThat(results)
            .extracting("a.action")
            .containsExactly("on create");

        // executing the same query - SET clause result is different
        results = submitAndGet(query);
        assertThat(results)
            .extracting("a.action")
            .containsExactly("on match");
    }

    @Test
    public void createRelationshipWhenAllMatchesFilteredOut() throws Exception {
        submitAndGet("CREATE (a:A), (b:B)");

        String query = "MATCH (a:A), (b:B) " +
            "MERGE (a)-[r:TYPE {name: 'r2'}]->(b) " +
            "RETURN count(r) AS count";

        // checking that relation alias is exported from CREATE
        List<Map<String, Object>> results = submitAndGet(query);
        assertThat(results)
            .extracting("count")
            .containsExactly(1L);

        // checking that relation alias is exported from MATCH
        results = submitAndGet(query);
        assertThat(results)
            .extracting("count")
            .containsExactly(1L);
    }

    @Test
    public void withMerge() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH 42 AS i " +
                "MERGE (n:N {value: i}) " +
                "RETURN n.value"
        );

        assertThat(results)
            .extracting("n.value")
            .containsExactly(42L);
    }

}
