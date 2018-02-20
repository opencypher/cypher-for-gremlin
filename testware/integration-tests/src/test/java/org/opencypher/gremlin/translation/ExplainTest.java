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
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithGremlinGroovy;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ExplainTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    /**
     * Explain only works with `cypher-gremlin-server-plugin`
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void explainIntegration() throws Exception {
        String cypher = "EXPLAIN\n" +
            "MATCH (s)-[:MEMBER_OF]->(ss)\n" +
            "RETURN ss.name AS system, collect(s.name) AS stars";
        List<Map<String, Object>> result = submitAndGet(cypher);

        assertThat(result).hasSize(1);

        Map<String, Object> explain = result.get(0);

        assertThat(explain.keySet())
            .containsExactly(
                "translation",
                "options"
            );
        assertThat(explain)
            .extracting(
                "options"
            )
            .containsExactly(
                "[EXPLAIN]"
            );
    }
}
