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
package org.opencypher.gremlin.traversal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opencypher.gremlin.translation.ReturnProperties.ID;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Before;
import org.junit.Test;
import org.opencypher.gremlin.client.CypherGremlinClient;

public class ReturnNormalizerTest {

    private TinkerGraph graph;
    private CypherGremlinClient client;

    @Before
    public void setUp() {
        graph = TinkerGraph.open();
        client = CypherGremlinClient.inMemory(graph.traversal());
    }

    @Test
    public void returnElementWithStringId() {
        String id = "stringId";
        graph.addVertex(T.id, id);
        List<Map<String, Object>> results = client.submit("MATCH (n) RETURN n").all();

        assertThat(results)
            .extracting("n")
            .extracting(ID)
            .containsExactly(id);
    }

    @Test
    public void returnStringId() {
        String id = "stringId";
        graph.addVertex(T.id, id);
        List<Map<String, Object>> results = client.submit("MATCH (n) RETURN id(n) AS id").all();

        assertThat(results)
            .extracting("id")
            .containsExactly(id);
    }
}
