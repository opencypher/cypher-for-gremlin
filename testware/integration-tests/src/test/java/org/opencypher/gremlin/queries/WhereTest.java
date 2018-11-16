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
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class WhereTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void nodeProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person)-[r:knows]->(friend:person)\n" +
                "WHERE n.name = \"marko\"\n" +
                "RETURN friend.name AS friend"
        );

        assertThat(results)
            .extracting("friend")
            .containsExactlyInAnyOrder("josh", "vadas");
    }

    @Test
    public void relationshipType() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)-[r]->(m)\n" +
                "WHERE type(r) = \"knows\"\n" +
                "RETURN n.name, m.name"
        );

        assertThat(results)
            .extracting("n.name", "m.name")
            .containsExactlyInAnyOrder(
                tuple("marko", "vadas"),
                tuple("marko", "josh")
            );
    }

    @Test
    public void relationshipTypeOnBothSides() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n1:person {name: 'marko'})-[r1]->(n2)<-[r2]-(n3) " +
                "WHERE type(r1) = type(r2) " +
                "RETURN n1.name, n2.name, n3.name"
        );

        assertThat(results)
            .extracting("n1.name", "n2.name", "n3.name")
            .containsExactlyInAnyOrder(
                tuple("marko", "lop", "peter"),
                tuple("marko", "lop", "josh")
            );
    }

    @Test
    public void comparison() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:person) " +
                "WHERE 29 <= p.age < 35 " +
                "RETURN p.name"
        );

        assertThat(results)
            .extracting("p.name")
            .containsExactlyInAnyOrder("marko", "josh");
    }

    @Test
    public void booleanOperators() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) " +
                "WHERE n.age = 29 OR (n.age > 30 AND NOT n.age = 32) " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("marko", "peter");
    }

    @Test
    public void notFalse() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "WHERE NOT(false) " +
                "RETURN n"
        );

        assertThat(results)
            .hasSize(6);
    }

    @Test
    public void notAndFalse() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:software) " +
                "WHERE NOT(n.name = 'lop' AND false) " +
                "RETURN n.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("lop", "ripple");
    }

    @Test
    public void pathLength() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH p = (n)-[r:knows]->(m) " +
                "WHERE length(p) = 1 " +
                "RETURN p"
        );

        assertThat(results)
            .hasSize(2);
    }

    @Test
    public void pattern() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) " +
                "WHERE (n)-[:created]->(:software) " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("marko", "josh", "peter");
    }

    @Test
    public void reversePattern() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) " +
                "WHERE (:software)<-[:created]-(n) " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("marko", "josh", "peter");
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void constantsCustomPredicate() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:software) " +
                "WHERE 'foo' STARTS WITH 'f' " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("lop", "ripple");
    }

    @Test
    public void constantsInequality() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:software) " +
                "WHERE 1 <> 2 " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("lop", "ripple");
    }

}
