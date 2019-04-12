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
package org.opencypher.gremlin.snippets;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.driver.ser.GraphBinaryMessageSerializerV1;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.client.CypherResultSet;
import org.opencypher.gremlin.client.CypherTraversalSource;
import org.opencypher.gremlin.client.GremlinClientFactory;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFeature;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class CypherGremlinServerClientSnippets {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    @Test
    public void demo() throws Exception {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("port", gremlinServer.getPort());
        configuration.setProperty("hosts", singletonList("localhost"));
        configuration.setProperty("serializer.className", GraphBinaryMessageSerializerV1.class.getName());

        // freshReadmeSnippet: demo
        String cypher = "MATCH (p:person) WHERE p.age > 25 RETURN p.name";

        Cluster cluster = Cluster.open(configuration);
        Client gremlinClient = cluster.connect();

        // Server has Gremlin Server plugin installed
        // Send Cypher to server
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.plugin(gremlinClient);
        List<Map<String, Object>> cypherResults = cypherGremlinClient.submit(cypher).all();

        // Client side translation
        // Send Gremlin to server
        CypherGremlinClient translatingGremlinClient = CypherGremlinClient.translating(gremlinClient);
        List<Map<String, Object>> gremlinResults = translatingGremlinClient.submit(cypher).all();

        assertThat(cypherResults).isEqualTo(gremlinResults);

        // freshReadmeSnippet: demo

        assertThat(gremlinResults)
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void gremlinStyle() throws Exception {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("port", gremlinServer.getPort());
        configuration.setProperty("hosts", singletonList("localhost"));
        configuration.setProperty("serializer.className", GraphBinaryMessageSerializerV1.class.getName());

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
        Client gremlinClient = GremlinClientFactory.create(gremlinServer.getPort());

        // freshReadmeSnippet: translating
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(gremlinClient);
        // freshReadmeSnippet: translating

        List<Map<String, Object>> results = cypherGremlinClient.submit("MATCH (p:person) WHERE p.age > 25 RETURN p.name").all();
        assertThat(results)
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void neptune() {
        Client gremlinClient = GremlinClientFactory.create(gremlinServer.getPort());

        // freshReadmeSnippet: neptune
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(
            gremlinClient,
            () -> Translator.builder()
                .gremlinGroovy()
                .inlineParameters()
                .enableMultipleLabels()
                .build(TranslatorFlavor.neptune())
        );
        // freshReadmeSnippet: neptune

        List<Map<String, Object>> results = cypherGremlinClient.submit("MATCH (p:person) WHERE p.age > 25 RETURN p.name").all();
        assertThat(results)
            .extracting("p.name")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void cosmosDb() {
        Client gremlinClient = GremlinClientFactory.create(gremlinServer.getPort());

        // freshReadmeSnippet: cosmosdb
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.retrieving(
            gremlinClient,
            TranslatorFlavor.cosmosDb()
        );
        // freshReadmeSnippet: cosmosdb

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

    @Test
    public void cypherTraversalSource() {
        TinkerGraph graph = TinkerFactory.createModern();

        // freshReadmeSnippet: cypherTraversalSource
        CypherTraversalSource g = graph.traversal(CypherTraversalSource.class);

        GraphTraversal<Map<String, Object>, String> query = g
            .cypher("MATCH (n) RETURN n")
            .select("n")
            .outE()
            .label()
            .dedup();
        // freshReadmeSnippet: cypherTraversalSource

        List<String> results = query.toList();

        assertThat(results)
            .containsExactlyInAnyOrder("knows", "created");
    }

    @Test
    public void cypherTraversalSourceWithRemote() throws Throwable {
        String PATH_TO_REMOTE_PROPERTIES = gremlinServer.driverRemoteConfiguration();

        // freshReadmeSnippet: cypherTraversalWithRemote
        CypherTraversalSource g = AnonymousTraversalSource
            .traversal(CypherTraversalSource.class)
            .withRemote(PATH_TO_REMOTE_PROPERTIES);

        GraphTraversal<Map<String, Object>, String> traversal = g
            .cypher("MATCH (n) RETURN n", "cosmosdb")
            .select("n")
            .outE()
            .label()
            .dedup();
        // freshReadmeSnippet: cypherTraversalWithRemote

        List<String> results = traversal.toList();

        assertThat(results)
            .containsExactlyInAnyOrder("created", "knows");
    }

    @Test
    public void translatorEnableExperimental() throws Exception {
        Client gremlinClient = GremlinClientFactory.create(gremlinServer.getPort());

        // freshReadmeSnippet: enableExperimentalGremlin
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.translating(
            gremlinClient,
            () -> Translator.builder()
                .gremlinGroovy()
                .enableCypherExtensions()
                .enable(TranslatorFeature.EXPERIMENTAL_GREMLIN_FUNCTION)
                .build()
        );

        List<Map<String, Object>> results = cypherGremlinClient.submit(
            "MATCH (n:person {name: 'marko'}) " +
                "RETURN gremlin(\"select('n').outE().label()\") as r").all();
        // freshReadmeSnippet: enableExperimentalGremlin

        assertThat(results)
            .extracting("r")
            .containsExactly("created");
    }

    @Test
    public void gremlinClient() throws Exception {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("port", gremlinServer.getPort());
        configuration.setProperty("hosts", singletonList("localhost"));
        configuration.setProperty("serializer.className", GraphBinaryMessageSerializerV1.class.getName());

        // freshReadmeSnippet: gremlinClient
        Cluster cluster = Cluster.open(configuration);
        Client client = cluster.connect();

        String cypherQuery = "MATCH (n) RETURN n.name";
        RequestMessage request = RequestMessage.build(Tokens.OPS_EVAL)
            .processor("cypher")
            .add(Tokens.ARGS_GREMLIN, cypherQuery)
            .create();

        ResultSet results = client.submitAsync(request).get();
        // freshReadmeSnippet: gremlinClient

        assertThat(results)
            .extracting(Result::getObject)
            .extracting("n.name")
            .containsExactlyInAnyOrder("marko", "vadas", "lop", "josh", "ripple", "peter");
    }
}
