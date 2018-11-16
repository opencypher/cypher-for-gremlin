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

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class CastTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castToString() throws Exception {
        List<Map<String, Object>> results = Stream.of(
            "13",
            "3.14",
            "'Hello'",
            "true",
            "null"
        )
            .flatMap(literal -> submitAndGet(
                "WITH " + literal + " AS l " +
                    "RETURN toString(l) AS r"
            ).stream())
            .collect(toList());

        assertThat(results)
            .extracting("r")
            .containsExactly(
                "13",
                "3.14",
                "Hello",
                "true",
                null
            );
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castInvalidToString() throws Exception {
        List<Throwable> throwables = Stream.of(
            "[]",
            "{a: 1}"
        )
            .map(literal -> catchThrowable(() -> submitAndGet(
                "WITH [1, " + literal + "] AS list\n" +
                    "RETURN toString(list[1]) AS n"
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("Cannot convert"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castToInteger() throws Exception {
        List<Map<String, Object>> results = Stream.of(
            "13",
            "9007199254740993", // Cannot be stored as Double
            "3.14",
            "'13'",
            "'3.14'",
            "'Hello'",
            "null"
        )
            .flatMap(literal -> submitAndGet(
                "WITH " + literal + " AS l " +
                    "RETURN toInteger(l) AS r"
            ).stream())
            .collect(toList());

        assertThat(results)
            .extracting("r")
            .containsExactly(
                13L,
                9007199254740993L,
                3L,
                13L,
                3L,
                null,
                null
            );
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castInvalidToInteger() throws Exception {
        List<Throwable> throwables = Stream.of(
            "true",
            "false",
            "[]",
            "{a: 1}"
        )
            .map(literal -> catchThrowable(() -> submitAndGet(
                "WITH [1, " + literal + "] AS list\n" +
                    "RETURN toInteger(list[1]) AS n"
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("Cannot convert"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castToFloat() throws Exception {
        List<Map<String, Object>> results = Stream.of(
            "13",
            "3.14",
            "'13'",
            "'3.14'",
            "'Hello'",
            "null"
        )
            .flatMap(literal -> submitAndGet(
                "WITH " + literal + " AS l " +
                    "RETURN toFloat(l) AS r"
            ).stream())
            .collect(toList());

        assertThat(results)
            .extracting("r")
            .containsExactly(
                13.0,
                3.14,
                13.0,
                3.14,
                null,
                null
            );
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castInvalidToFloat() throws Exception {
        List<Throwable> throwables = Stream.of(
            "true",
            "false",
            "[]",
            "{a: 1}"
        )
            .map(literal -> catchThrowable(() -> submitAndGet(
                "WITH [1.0, " + literal + "] AS list\n" +
                    "RETURN toFloat(list[1]) AS n"
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("Cannot convert"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castToBoolean() throws Exception {
        List<Map<String, Object>> results = Stream.of(
            "true",
            "false",
            "'True'",
            "'False'",
            "'13'",
            "'3.14'",
            "'Hello'",
            "null"
        )
            .flatMap(literal -> submitAndGet(
                "WITH " + literal + " AS l " +
                    "RETURN toBoolean(l) AS r"
            ).stream())
            .collect(toList());

        assertThat(results)
            .extracting("r")
            .containsExactly(
                true,
                false,
                true,
                false,
                null,
                null,
                null,
                null
            );
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void castInvalidToBoolean() throws Exception {
        List<Throwable> throwables = Stream.of(
            "13",
            "3.14",
            "[]",
            "{a: 1}"
        )
            .map(literal -> catchThrowable(() -> submitAndGet(
                "WITH [true, " + literal + "] AS list\n" +
                    "RETURN toBoolean(list[1]) AS n"
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("Cannot convert"));
    }
}
