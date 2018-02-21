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
package org.opencypher.gremlin.snippets;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.client.CypherResultSet;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class CypherGremlinServerClient {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Test
    public void gremlinStyle() throws Exception {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("port", gremlinServer.getPort());
        configuration.setProperty("hosts", Arrays.asList("localhost"));

        // freshReadmeSnippet: gremlinStyle
        Cluster cluster = Cluster.open(configuration);
        Client gremlinClient = cluster.connect();
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.plugin(gremlinClient);
        String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
        CypherResultSet resultSet = cypherGremlinClient.submit(cypher);
        List<Map<String, Object>> results = resultSet.all();
        // freshReadmeSnippet: gremlinStyle

        assertThat(results)
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void workingWithCypherGremlinClient() {
        String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
        CypherGremlinClient cypherGremlinClient = gremlinServer.cypherGremlinClient();

        // freshReadmeSnippet: workingWithCypherGremlinClient
        List<Map<String, Object>> list = cypherGremlinClient.submit(cypher).all(); // as a list
        Stream<Map<String, Object>> stream = cypherGremlinClient.submit(cypher).stream(); // as a stream
        Iterator<Map<String, Object>> iterator = cypherGremlinClient.submit(cypher).iterator(); // as an iterator
        Iterable<Map<String, Object>> iterable = cypherGremlinClient.submit(cypher); // also an iterable
        // freshReadmeSnippet: workingWithCypherGremlinClient

        assertThat(list)
            .isEqualTo(stream.collect(Collectors.toList()))
            .isEqualTo(Lists.newArrayList(iterator))
            .isEqualTo(Lists.newArrayList(iterable))
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void async() {
        CypherGremlinClient cypherGremlinClient = gremlinServer.cypherGremlinClient();

        // freshReadmeSnippet: async
        String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
        CompletableFuture<CypherResultSet> future = cypherGremlinClient.submitAsync(cypher);
        future
            .thenApply(CypherResultSet::all)
            .thenAccept(resultSet -> {
                // ...
            });
        // freshReadmeSnippet: async
    }

    @Test
    public void translating() {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("port", gremlinServer.getPort());
        configuration.setProperty("hosts", Arrays.asList("localhost"));
        Cluster cluster = Cluster.open(configuration);
        Client gremlinClient = cluster.connect();

        // freshReadmeSnippet: translating
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(gremlinClient);
        // freshReadmeSnippet: translating

        List<Map<String, Object>> results = cypherGremlinClient.submit("MATCH (p:person) WHERE p.age > 25 RETURN p.name").all();
        assertThat(results)
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void inMemory() {
        // freshReadmeSnippet: inMemory
        TinkerGraph graph = TinkerFactory.createModern();
        GraphTraversalSource traversal = graph.traversal();
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.inMemory(traversal);
        String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";
        List<Map<String, Object>> results = cypherGremlinClient.submit(cypher).all();
        // freshReadmeSnippet: inMemory

        assertThat(results)
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }


}
