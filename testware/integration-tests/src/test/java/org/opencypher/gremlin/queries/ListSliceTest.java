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
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ListSliceTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Before
    public void setUp() {
        submitAndGet(DELETE_ALL);
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return submitAndGet(cypher, emptyMap());
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameters).all();
    }

    @Test
    public void listRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3, 4, 5] AS list " +
                "RETURN list[1..3] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(2L, 3L));
    }

    @Test
    public void listRangeImplicitEnd() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[1..] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(2L, 3L));
    }

    @Test
    public void listRangeImplicitStart() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[..2] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(1L, 2L));
    }

    @Test
    public void listSingletonRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[0..1] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(singletonList(1L));
    }

    @Test
    public void listEmptyRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[0..0] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(emptyList());
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listNegativeRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[-3..-1] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(1L, 2L));
    }

    @Test
    public void listInvalidRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[3..1] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(emptyList());
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listExceedingRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[-5..5] AS r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(1L, 2L, 3L));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listRangeParametrized() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[$from..$to] AS r",
            new HashMap<>(ImmutableMap.of("from", 1, "to", 3))
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(2L, 3L));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listParametrizedEmptyRange() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list " +
                "RETURN list[$from..$to] AS r",
            new HashMap<>(ImmutableMap.of("from", 3, "to", 1))
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(emptyList());
    }
}
