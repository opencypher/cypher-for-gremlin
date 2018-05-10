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

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ContainerIndexTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Before
    public void setUp() {
        gremlinServer.gremlinClient().submit("g.V().drop()").all().join();
    }

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
    public void nonExistentMapIndex() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: 1} AS map\n" +
                "RETURN map['ba' + 'r'] AS i"
        );
        assertThat(results)
            .extracting("i")
            .containsExactly((Object) null);
    }
}
