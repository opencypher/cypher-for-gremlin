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
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class FunctionTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void functionsCombination() throws Exception {
        String query = "MATCH (n) RETURN sum(size(keys(n))) AS totalNumberOfProps";
        Object count = submitAndGet(query).iterator().next().get("totalNumberOfProps");
        assertThat(count).isEqualTo(12L);
    }

    @Test
    public void returnCount() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN count(n)"
        );

        assertThat(results)
            .extracting("count(n)")
            .containsExactly(6L);

        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n:NONEXISTENT) RETURN count(n)"
        );

        assertThat(afterDelete)
            .extracting("count(n)")
            .containsExactly(0L);
    }

    @Test
    public void returnCountStar() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH () RETURN count(*)"
        );

        assertThat(results)
            .extracting("count(*)")
            .containsExactly(6L);
    }

    @Test
    public void countCapitalized() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "RETURN Count(n) as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactly(6L);
    }

    @Test
    public void typeCapitalized() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)-[r]->(m) " +
                "WHERE Type(r) = 'knows' " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("marko", "marko");
    }

    @Test
    public void existsCapitalized() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "WHERE Exists(n.age) " +
                "RETURN n.name"
        );

        assertThat(results)
            .extracting("n.name")
            .containsExactlyInAnyOrder("marko", "vadas", "josh", "peter");
    }

    @Test
    public void existsInReturn() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "RETURN exists(n.age) as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(true, true, false, true, false, true);
    }

    @Test
    public void rangeCapitalized() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND Range(1, 3) AS r " +
                "RETURN r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(1L, 2L, 3L);
    }

    @Test
    public void sizeOfList() {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN size(['Alice', 'Bob']) as size"
        );

        assertThat(results)
            .extracting("size")
            .containsExactly(2L);
    }

    @Test
    public void sizeOfString() {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN size('Alice') as size"
        );

        assertThat(results)
            .extracting("size")
            .containsExactly(5L);
    }

    @Test
    public void sizeOfPatternExpressionInReturn() {
        String cypher = "MATCH (a) " +
            "WHERE a.name = 'marko' " +
            "RETURN size((a)-->()-->()) AS fof";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("fof")
            .containsExactly(2L);
    }

    @Test
    public void head() {
        String cypher = "MATCH (n:person) WITH n.name AS name " +
            "ORDER BY name RETURN head(collect(name)) AS head";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("head")
            .containsExactly("josh");
    }

    @Test
    public void headNotExistingProperties() {
        String cypher = "MATCH (n:person {name: 'marko'}) RETURN head(n.notExisting) as head";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("head")
            .containsNull();
    }

    @Test
    public void headNull() {
        String cypher = "RETURN head(null)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("head")
            .containsNull();
    }

    @Test
    public void headEmpty() {
        String cypher = "MATCH (n:notExising) WITH n AS n RETURN head(collect(n)) AS head";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("head")
            .containsNull();
    }

    @Test
    public void tail() {
        String cypher = "MATCH (n:person) WITH n.name AS name " +
            "ORDER BY name RETURN tail(collect(name)) AS tail";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("tail")
            .containsExactly(asList("marko", "peter", "vadas"));
    }

    @Test
    public void tailNotExistingProperties() {
        String cypher = "MATCH (n:person {name: 'marko'}) RETURN tail(n.notExisting) as tail";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("tail")
            .containsNull();
    }

    @Test
    public void tailNull() {
        String cypher = "RETURN tail(null)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("head")
            .containsNull();
    }

    @Test
    public void tailEmpty() {
        String cypher = "MATCH (n:notExisting) WITH n AS n RETURN tail(collect(n)) AS tail";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("tail")
            .containsExactly(new ArrayList<>());
    }

    @Test
    public void last() {
        String cypher = "MATCH (n:person) WITH n.name AS name " +
            "ORDER BY name RETURN last(collect(name)) AS last";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("last")
            .containsExactly("vadas");
    }

    @Test
    public void lastNotExistingProperties() {
        String cypher = "MATCH (n:person {name: 'marko'}) RETURN last(n.notExisting) as last";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("last")
            .containsNull();
    }

    @Test
    public void lastNull() {
        String cypher = "RETURN last(null)";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("last")
            .containsNull();
    }

    @Test
    public void lastEmpty() {
        String cypher = "MATCH (n:notExising) WITH n AS n RETURN last(collect(n)) AS last";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("last")
            .containsNull();
    }

    @Test
    public void sizeOfPatternExpressionInWhere() {
        String cypher = "MATCH (n:person) WHERE size( (n)-->() ) > 1 RETURN n.name";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("n.name")
            .containsExactly("marko", "josh");
    }

    @Test
    public void propertiesOnNode() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n {name:'marko'}) RETURN properties(n) as r");

        assertThat(results)
            .extracting("r")
            .containsExactly(ImmutableMap.of("age", 29L, "name", "marko"));
    }
}
