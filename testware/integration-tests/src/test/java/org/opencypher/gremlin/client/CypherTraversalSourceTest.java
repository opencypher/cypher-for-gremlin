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
package org.opencypher.gremlin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Before;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.translation.Tokens;


public class CypherTraversalSourceTest {
    private CypherTraversalSource g;

    @Before
    public void setUp() throws Exception {
        TinkerGraph graph = TinkerFactory.createModern();
        g = graph.traversal(CypherTraversalSource.class);
    }

    @Test
    public void returnNulls() {
        GraphTraversal<Map<String, Object>, Object> traversal = g
            .cypher("MATCH (n:software) RETURN n.age as age")
            .select("age");

        List<Object> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder(Tokens.NULL, Tokens.NULL);
    }

    @Test
    public void returnValuesNull() {
        GraphTraversal<Map<String, Object>, Map<String, Object>> traversal = g
            .cypher("MATCH (n) RETURN n.age as age");

        List<Map<String, Object>> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder(
                ImmutableMap.of("age", 29),
                ImmutableMap.of("age", 27),
                ImmutableMap.of("age", Tokens.NULL),
                ImmutableMap.of("age", 32),
                ImmutableMap.of("age", Tokens.NULL),
                ImmutableMap.of("age", 35)
            );
    }

    @Test
    public void parameters() {
        GraphTraversal<Map<String, Object>, Map<String, Object>> traversal = g
            .cypher("MATCH (n:person) " +
                "WHERE n.name = $name " +
                "RETURN n.age", ImmutableMap.of("name", "marko"));

        List<Map<String, Object>> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder(
                ImmutableMap.of("n.age", 29)
            );
    }

    @Test
    public void continueValues() {
        GraphTraversal<Map<String, Object>, Object> traversal = g
            .cypher("MATCH (n) RETURN n.age as age")
            .select("age")
            .is(P.neq(Tokens.NULL))
            .is(P.gt(0))
            .fold()
            .unfold();

        List<Object> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder(29, 27, 32, 35);
    }

    @Test
    public void continueElements() {
        GraphTraversal<Map<String, Object>, String> traversal = g
            .cypher("MATCH (n) RETURN n")
            .select("n")
            .outE()
            .label()
            .dedup();

        List<String> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder("knows", "created");
    }

    @Test
    public void translate() {
        assertThatThrownBy(() -> g.cypher("RETURN toupper('test')", "cosmosdb"))
            .hasMessageContaining("Custom functions and predicates are not supported: cypherToUpper");
    }


    @Test
    public void withRemote () throws Throwable {
        GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::snGraph);
        gremlinServer.before();

        Cluster cluster = gremlinServer.gremlinClient().getCluster();
        CypherTraversalSource g = EmptyGraph.instance().traversal(CypherTraversalSource.class).withRemote(DriverRemoteConnection.using(cluster));

        GraphTraversal<Map<String, Object>, String> traversal = g
            .cypher("MATCH (n) RETURN n")
            .select("n")
            .outE()
            .label()
            .dedup();

        List<String> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder("IS_PART_OF", "IS_LOCATED_IN", "KNOWS", "LIKES");
    }
}
