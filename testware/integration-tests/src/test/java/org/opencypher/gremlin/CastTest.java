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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CastTest {

    @ClassRule
    public static final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher);
    }

    @Test
    public void castToString() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [13, 3.14, 'Hello', true, null] AS n\n" +
                "RETURN toString(n) AS r"
        );
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
    public void castToInteger() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [13, 3.14, '13', '3.14', 'Hello', null] AS n\n" +
                "RETURN toInteger(n) AS r"
        );
        assertThat(results)
            .extracting("r")
            .containsExactly(
                13L,
                3L,
                13L,
                3L,
                null,
                null
            );
    }

    @Test
    public void castInvalidToInteger() throws Exception {
        List<Throwable> throwables = Stream.of(
            "true",
            "false",
            "[]",
            "{}"
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
    public void castToFloat() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [13, 3.14, '13', '3.14', 'Hello', null] AS n\n" +
                "RETURN toFloat(n) AS r"
        );
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
    public void castInvalidToFloat() throws Exception {
        List<Throwable> throwables = Stream.of(
            "true",
            "false",
            "[]",
            "{}"
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
    public void castToBoolean() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [true, false, 'True', 'False', '13', '3.14', 'Hello', null] AS n\n" +
                "RETURN toBoolean(n) AS r"
        );
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
    public void castInvalidToBoolean() throws Exception {
        List<Throwable> throwables = Stream.of(
            "13",
            "3.14",
            "[]",
            "{}"
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
