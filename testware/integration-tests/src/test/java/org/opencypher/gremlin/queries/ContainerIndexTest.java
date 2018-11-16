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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ContainerIndexTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return submitAndGet(cypher, emptyMap());
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameters).all();
    }

    @Test
    public void listIndexInReturn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN list[1] AS i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly(2L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listIndexInReturnFunction() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN toString(list[1]) AS s"
        );

        assertThat(results)
            .extracting("s")
            .containsExactly("2");
    }

    @Test
    public void listIndexOutOfBounds() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN list[5] AS i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly((Object) null);
    }

    @Test
    public void listIndexProjection() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "WITH list[0] AS i " +
                "RETURN i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly(1L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listNegativeIndex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[-1] AS i1, list[-3] AS i2"
        );

        assertThat(results)
            .extracting("i1", "i2")
            .containsExactly(tuple(3L, 1L));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void nonExistentListIndex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[3] AS n1, list[-4] AS n2"
        );

        assertThat(results)
            .extracting("n1", "n2")
            .containsExactly(tuple(null, null));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void nullList() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH null AS list\n" +
                "RETURN list[1] AS i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly((Object) null);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listInvalidIndex() throws Exception {
        List<Throwable> throwables = Stream.of(
            new HashMap<>(ImmutableMap.of("expr", emptyMap(), "idx", 1)),
            new HashMap<>(ImmutableMap.of("expr", emptyList(), "idx", "foo")),
            new HashMap<>(ImmutableMap.of("expr", 100, "idx", 0))
        )
            .map(params -> catchThrowable(() -> submitAndGet(
                "WITH $expr AS expr, $idx AS idx " +
                    "RETURN expr[idx]",
                params
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("element access"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listIndexOnAnyType() {
        submitAndGet("CREATE (:N {p1: 1, p2: 2})");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:N {p1: 1}) " +
                "WITH [n] AS l, 'p2' AS k " +
                "RETURN l[0][k] AS p2"
        );

        assertThat(results)
            .extracting("p2")
            .containsExactly(2L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void filterByListIndexOnAnyType() {
        submitAndGet("CREATE (:N {p: 1}), (:N {p: 2}), (:N)");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:N) " +
                "WITH n, [n] AS l, 'p' AS k " +
                "WHERE l[0][k] > 1 " +
                "RETURN n.p"
        );

        assertThat(results)
            .extracting("n.p")
            .containsExactly(2L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void mapIndexInReturn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: 1, bar: 2, baz: 3} AS map\n" +
                "RETURN map['ba' + 'r'] AS i"
        );

        assertThat(results)
            .extracting("i")
            .containsExactly(2L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void mapIndexInReturnFunction() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: 1, bar: 2, baz: 3} AS map\n" +
                "RETURN toString(map['ba' + 'r']) AS s"
        );

        assertThat(results)
            .extracting("s")
            .containsExactly("2");
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void nonExistentMapIndex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: 1} AS map\n" +
                "RETURN map['ba' + 'r'] AS i"
        );
        assertThat(results)
            .extracting("i")
            .containsExactly((Object) null);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void mapInMap() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: {bar: 'baz'}} AS map " +
                "RETURN map['foo']['bar'] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly("baz");
    }
}
