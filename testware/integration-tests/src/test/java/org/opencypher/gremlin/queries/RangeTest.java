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
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithJanusGraph;
import org.opencypher.gremlin.groups.SkipWithNeptune;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class RangeTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return submitAndGet(cypher, emptyMap());
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameters).all();
    }

    @Test
    public void fromZero() throws Exception {
        String cypher = "RETURN range(0, 2) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(0L, 1L, 2L));
    }

    @Test
    public void simple() throws Exception {
        String cypher = "RETURN range(1, 3) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(1L, 2L, 3L));
    }

    @Test
    public void withStep() throws Exception {
        String cypher = "RETURN range(3, 6, 2) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(3L, 5L));
    }

    @Test
    public void expression() throws Exception {
        String cypher = "WITH ['a', 'b', 'c'] AS a RETURN range(1, size(a) - 1) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(1L, 2L));
    }

    @Test
    public void twoExpressions() throws Exception {
        String cypher = "WITH ['a', 'b', 'c'] AS a RETURN range(size(a) - 1, size(a) + 1) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(2L, 3L, 4L));
    }

    @Test
    public void threeExpressions() throws Exception {
        String cypher = "WITH ['a', 'b', 'c'] AS a RETURN range(size(a) - 1, size(a) + 1, size(a) - 1) as r";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r")
            .containsExactly(asList(2L, 4L));
    }

    @Test
    public void keepTraversalHistory() throws Exception {
        String cypher = "WITH ['a', 'b'] AS a UNWIND range(1, 2) as r RETURN r, a";
        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("r", "a")
            .containsExactly(
                tuple(1L, asList("a", "b")),
                tuple(2L, asList("a", "b"))
            );
    }

    @Test
    public void compileTimeValidations() throws Exception {
        assertThatThrownBy(() -> submitAndGet("RETURN range(0, 10001) as r"))
            .hasMessageContaining("Range is too big");

        assertThatThrownBy(() -> submitAndGet("RETURN range(10000, 20001) as r"))
            .hasMessageContaining("Range is too big");

        assertThatThrownBy(() -> submitAndGet("RETURN range(-1, 10) as r"))
            .hasMessageContaining("Unsupported negative range start");

        assertThatThrownBy(() -> submitAndGet("RETURN range(0, -1) as r"))
            .hasMessageContaining("Unsupported negative range end");

        assertThatThrownBy(() -> submitAndGet("RETURN range(0, 1, 0) as r"))
            .hasMessageContaining("cannot be zero");

        assertThatThrownBy(() -> submitAndGet("RETURN range(0, 1, -1) as r"))
            .hasMessageContaining("Unsupported negative range steps");
    }

    @Test
    @Category(SkipWithNeptune.NoExceptionDetailMessage.class)
    public void runTimeValidations() throws Exception {
        assertThatThrownBy(() -> submitAndGet("WITH ['a'] AS a RETURN range(0, size(a) + 10000) as r"))
            .hasMessageContaining("Invalid range argument");

        assertThatThrownBy(() -> submitAndGet("WITH ['a'] AS a RETURN range(size(a) + 10000, size(a) + 20001) as r"))
            .hasMessageContaining("Invalid range argument");

        assertThatThrownBy(() -> submitAndGet("WITH ['a'] AS a RETURN range(size(a) - 2, 1) as r"))
            .hasMessageContaining("Invalid range argument");

        assertThatThrownBy(() -> submitAndGet("WITH ['a'] AS a RETURN range(0, size(a) - 2) as r"))
            .hasMessageContaining("Invalid range argument");

        assertThatThrownBy(() -> submitAndGet("WITH ['a'] AS a RETURN range(0, 1, size(a) - 1) as r"))
            .hasMessageContaining("Division by zero");

        assertThatThrownBy(() -> submitAndGet("WITH ['a'] AS a RETURN range(0, 1, size(a) - 2) as r"))
            .hasMessageContaining("Invalid range argument");
    }

    @Test
    @Category({SkipWithJanusGraph.NoExceptionDetailMessage.class, SkipWithNeptune.NoExceptionDetailMessage.class})
    public void runTimeNullValidations() throws Exception {
        assertThatThrownBy(() -> submitAndGet("WITH [null] AS a RETURN RANGE (a[0], 3, 1)"))
            .hasStackTraceContaining("cannot be cast");

        assertThatThrownBy(() -> submitAndGet("WITH [null] AS a RETURN RANGE (0, a[0], 1)"))
            .hasStackTraceContaining("cannot be cast");

        assertThatThrownBy(() -> submitAndGet("WITH [null] AS a RETURN RANGE (0, 3, a[0])"))
            .hasStackTraceContaining("cannot be cast");
    }

}
