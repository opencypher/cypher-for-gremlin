/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.gremlin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Before;
import org.junit.Test;

public class InMemoryCypherGremlinClientTest {

    private InMemoryCypherGremlinClient client;

    @Before
    public void setUp() {
        TinkerGraph graph = TinkerGraph.open();
        client = new InMemoryCypherGremlinClient(graph.traversal());
    }

    @Test
    public void createAndMatch() {
        client.submit("CREATE (:L {foo: 'bar'})");
        List<Map<String, Object>> results = client.submit("MATCH (n:L) return n.foo").all();

        assertThat(results)
            .extracting("n.foo")
            .containsExactly("bar");
    }

    @Test
    public void invalidSyntax() {
        CypherResultSet resultSet = client.submit("INVALID");
        Throwable throwable = catchThrowable(resultSet::all);

        assertThat(throwable)
            .hasMessageContaining("Invalid input");
    }
}
