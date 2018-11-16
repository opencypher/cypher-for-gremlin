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

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

@Category(UsesExtensions.CustomFunctions.class)
public class PercentileTest {

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
    public void percentileCont() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [10, 20, 30] AS i " +
                "RETURN " +
                "percentileCont(i, 0.0) AS p0, " +
                "percentileCont(i, 0.33) AS p33, " +
                "percentileCont(i, 0.5) AS p50, " +
                "percentileCont(i, 0.66) AS p66, " +
                "percentileCont(i, 1.0) AS p100"
        );

        assertThat(results)
            .extracting("p0", "p33", "p50", "p66", "p100")
            .containsExactly(tuple(10L, 16.6, 20L, 23.2, 30L));
    }

    @Test
    public void percentileContSingle() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [10] AS i " +
                "RETURN percentileCont(i, 0.5) AS p50"
        );

        assertThat(results)
            .extracting("p50")
            .containsExactly(10L);
    }

    @Test
    public void percentileContEmpty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [] AS i " +
                "RETURN percentileCont(i, 0.5) AS p50"
        );

        assertThat(results)
            .extracting("p50")
            .containsExactly((Object) null);
    }

    @Test
    public void percentileContInvalidArgument() throws Exception {
        submitAndGet("CREATE ({prop: 10.0})");
        List<Throwable> throwables = Stream.of(1000, -1, 1.1)
            .map(param -> catchThrowable(() -> submitAndGet(
                "MATCH (n) RETURN percentileCont(n.prop, $param)",
                Collections.singletonMap("param", param)
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("Number out of range"));
    }

    @Test
    public void percentileDisc() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [10, 20, 30] AS i " +
                "RETURN " +
                "percentileDisc(i, 0.0) AS p0, " +
                "percentileDisc(i, 0.33) AS p33, " +
                "percentileDisc(i, 0.34) AS p34, " +
                "percentileDisc(i, 0.5) AS p50, " +
                "percentileDisc(i, 0.66) AS p66, " +
                "percentileDisc(i, 0.67) AS p67, " +
                "percentileDisc(i, 1.0) AS p100"
        );

        assertThat(results)
            .extracting("p0", "p33", "p34", "p50", "p66", "p67", "p100")
            .containsExactly(tuple(10L, 10L, 20L, 20L, 20L, 30L, 30L));
    }

    @Test
    public void percentileDiscSingle() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [10] AS i " +
                "RETURN percentileDisc(i, 0.5) AS p50"
        );

        assertThat(results)
            .extracting("p50")
            .containsExactly(10L);
    }

    @Test
    public void percentileDiscEmpty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [] AS i " +
                "RETURN percentileDisc(i, 0.5) AS p50"
        );

        assertThat(results)
            .extracting("p50")
            .containsExactly((Object) null);
    }

    @Test
    public void percentileDiscInvalidArgument() throws Exception {
        submitAndGet("CREATE ({prop: 10.0})");
        List<Throwable> throwables = Stream.of(1000, -1, 1.1)
            .map(param -> catchThrowable(() -> submitAndGet(
                "MATCH (n) RETURN percentileDisc(n.prop, $param)",
                Collections.singletonMap("param", param)
            )))
            .collect(toList());

        assertThat(throwables)
            .allSatisfy(throwable ->
                assertThat(throwable)
                    .hasMessageContaining("Number out of range"));
    }
}
