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
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FunctionTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher);
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

}
