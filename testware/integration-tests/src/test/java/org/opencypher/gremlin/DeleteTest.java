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

import org.junit.Rule;
import org.junit.Test;
import org.opencypher.gremlin.rules.TinkerGraphServerEmbedded;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteTest {

    @Rule
    public final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher);
    }

    @Test
    public void detachDelete() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (s:software) RETURN count(s)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (s:software) DETACH DELETE s"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (s:software) RETURN count(s)"
        );

        assertThat(beforeDelete)
            .extracting("count(s)")
            .containsExactly(2L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(s)")
            .containsExactly(0L);
    }

    @Test
    public void detachDeleteMultiple() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (s:software), (p:person) DETACH DELETE s, p"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }

    @Test
    public void deleteWithReturn() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");
        submitAndGet("CREATE ()-[:R]->()");
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (a)-[r]-(b) DELETE r, a, b RETURN count(*)"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(2L);
        assertThat(onDelete)
            .extracting("count(*)")
            .containsExactly(2L);
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }

    @Test
    public void deleteOptionalMatch() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");
        submitAndGet("CREATE ()");
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        submitAndGet(
            "MATCH (n)\n" +
                "OPTIONAL MATCH (n)-[r]-()\n" +
                "DELETE n, r"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(1L);
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }
}
