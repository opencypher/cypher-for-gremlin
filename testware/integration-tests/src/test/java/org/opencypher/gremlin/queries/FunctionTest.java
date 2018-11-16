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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class FunctionTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

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
    @Category(UsesExtensions.CustomFunctions.class)
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
            .containsExactlyInAnyOrder("marko", "josh");
    }

    @Test
    public void propertiesOnNode() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n {name:'marko'}) RETURN properties(n) as r");

        assertThat(results)
            .extracting("r")
            .containsExactly(ImmutableMap.of("age", 29L, "name", "marko"));
    }

    @Test
    public void startNode() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH ()-[r:knows]->()\n" +
                "RETURN startNode(r).name as r");

        assertThat(results)
            .extracting("r")
            .containsExactly("marko", "marko");
    }

    @Test
    public void endNode() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH ()-[r:knows]->()\n" +
                "RETURN endNode(r).name as r");

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("josh", "vadas");
    }

    @Test
    public void startEndNode() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH ()-[r:knows]-()-[s:created]-()\n" +
                "RETURN startNode(r).name as a, endNode(r).name as b, startNode(s).name as c, endNode(s).name as d");

        assertThat(results)
            .extracting("a", "b", "c", "d")
            .containsExactlyInAnyOrder(
                tuple("marko", "josh", "josh", "lop"),
                tuple("marko", "josh", "josh", "ripple"),
                tuple("marko", "vadas", "marko", "lop"),
                tuple("marko", "josh", "marko", "lop")
            );
    }

    @Test
    public void optionalStartEndNode() {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH ()-[r:notExisting]-()\n" +
                "RETURN startNode(r) as a, endNode(r) as b");

        assertThat(results)
            .extracting("a", "b")
            .containsExactlyInAnyOrder(tuple(null, null));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void stringFunctions() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH \"wOrD\" as m RETURN " +
                "upper(m) as u," +
                "lower(m) as l," +
                "reverse(m) as r");

        assertThat(results)
            .extracting("u", "l", "r")
            .containsExactlyInAnyOrder(tuple("WORD", "word", "DrOw"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void nullInStringFunctions() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (m {name: 'marko'}) RETURN " +
                "upper(m.notExisting) as u," +
                "lower(m.notExisting) as l," +
                "reverse(m.notExisting) as r");

        assertThat(results)
            .extracting("u", "l", "r")
            .containsExactlyInAnyOrder(tuple(null, null, null));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void invalidArgumentInStringFunctions() {
        assertThatThrownBy(() -> submitAndGet("MATCH (n {name: 'marko'}) RETURN tolower(n.age)"))
                        .hasMessageContaining("Expected a String value for <function1>, but got:");

        assertThatThrownBy(() -> submitAndGet("MATCH (n {name: 'marko'}) RETURN split(n.age, '1')"))
                        .hasMessageContaining("Expected a String value for <function1>, but got:");

        assertThatThrownBy(() -> submitAndGet("MATCH (n {name: 'marko'}) RETURN split('word', n.age)"))
                        .hasMessageContaining("Expected a String value for <function1>, but got:");

        assertThatThrownBy(() -> submitAndGet("MATCH (n {name: 'marko'}) RETURN reverse(n.age)"))
                        .hasMessageContaining("Expected a string or list value for reverse, but got:");
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void reverseList() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] as m RETURN reverse(m) as r");

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder(asList(3L, 2L, 1L));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void split() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH \"wOrD\" as m RETURN " +
                "split(m, 'O') as s1," +
                "split(m, 'x') as s2," +
                "split(null, m) as s3," +
                "split(m, null) as s4," +
                "split('', 'x') as s5");

        assertThat(results)
            .extracting("s1", "s2", "s3", "s4", "s5")
            .containsExactlyInAnyOrder(tuple(asList("w", "rD"), asList("wOrD"), null, null, asList("")));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void subString() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH \"wOrDwEb\" as m RETURN " +
                "substring(m, 0, 2) as s0," +
                "substring(m, 1, 2) as s1," +
                "substring(m, 1, 4) as s2," +
                "substring(m, 1, 6) as s3," +
                "substring(m, 1, 50) as s4," +
                "substring(m, 1) as s5, " +
                "substring(null, 1) as s6");

        assertThat(results)
            .extracting("s0", "s1", "s2", "s3", "s4", "s5", "s6")
            .containsExactlyInAnyOrder(tuple("wO","Or", "OrDw", "OrDwEb", "OrDwEb", "OrDwEb", null));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void subStringValidation() {
        assertThatThrownBy(() -> submitAndGet("RETURN substring('string', -1)"))
                        .hasMessageContaining("String index out of range: -1");

        assertThatThrownBy(() -> submitAndGet("RETURN substring('string', -1, 2)"))
                        .hasMessageContaining("String index out of range: -1");

        assertThatThrownBy(() -> submitAndGet("RETURN substring('string', 1, -1)"))
                        .hasMessageContaining("String index out of range: -1");

        assertThatThrownBy(() -> submitAndGet("MATCH (m {name: 'marko'}) RETURN substring('s', m.notExisting)"))
                        .hasMessageContaining("Expected substring(String, Integer, [Integer]), but got: (s,   cypher.null)");

        assertThatThrownBy(() -> submitAndGet("MATCH (m {name: 'marko'}) RETURN substring('s', m.notExisting, 2)"))
                        .hasMessageContaining("Expected substring(String, Integer, [Integer]), but got: (s,   cypher.null)");

        assertThatThrownBy(() -> submitAndGet("MATCH (m {name: 'marko'}) RETURN substring('s', 1, m.notExisting)"))
                        .hasMessageContaining("Expected substring(String, Integer, [Integer]), but got: (s, 1,   cypher.null)");

        assertThatThrownBy(() -> submitAndGet("MATCH (m {name: 'marko'}) RETURN substring(m.age, 1, 2)"))
                        .hasMessageContaining("Expected substring(String, Integer, [Integer]), but got: (29, 1)");
    }

}
