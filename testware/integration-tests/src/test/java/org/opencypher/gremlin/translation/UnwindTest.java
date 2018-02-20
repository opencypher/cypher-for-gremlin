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
package org.opencypher.gremlin.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class UnwindTest {
    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void unwindLabels() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) UNWIND labels(n) AS label RETURN DISTINCT label"
        );

        assertThat(results)
            .hasSize(2)
            .extracting("label")
            .containsExactlyInAnyOrder(
                "person",
                "software"
            );
    }

    @Test
    public void injectRange() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(1, 9) AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(45L);

    }

    @Test
    public void injectRangeWithStep() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(1, 9, 2) AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(25L);
    }

    @Test
    public void injectLargeRange() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(10001, 20000) AS i " +
                "RETURN sum(i) AS sum"
        );

        assertThat(results)
            .extracting("sum")
            .containsExactly(150005000L);
    }
}
