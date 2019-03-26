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

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.concurrent.ExecutionException;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.opencypher.gremlin.neo4j.driver.Config;
import org.opencypher.gremlin.neo4j.driver.GremlinDatabase;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class CypherGremlinNeo4jDriverSnippets {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Test
    public void demo() throws Exception {
        String uri = "//localhost:" + gremlinServer.getPort();

        // freshReadmeSnippet: demo
        Driver driver = GremlinDatabase.driver(uri);

        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (n) RETURN count(n) as count");
            int n = result.single().get("count").asInt();

            assertThat(n).isEqualTo(0); // 0
        }
        // freshReadmeSnippet: demo
    }

    @Test
    public void createDriver() throws Exception {
        String pathToGremlinConfiguration = "../../tinkerpop/cypher-gremlin-server-plugin/src/main/resources/local-gremlin-server.yaml";

        // freshReadmeSnippet: createDriver
        Cluster cluster1 = Cluster.build()
            .enableSsl(true)
            .addContactPoints("192.168.0.145")
            //...
            .create();
        Driver driver1 = GremlinDatabase.driver(cluster1);

        // Or:
        Cluster cluster2 = Cluster.open(pathToGremlinConfiguration);

        Driver driver2 = GremlinDatabase.driver(cluster2);
        // freshReadmeSnippet: createDriver
    }

    @Test
    public void createUseDriver() throws Exception {
        int port = gremlinServer.getPort();
        // freshReadmeSnippet: createConfiguration
        Config config = Config.build()
            .withTranslation()
            .toConfig();

        String uri = "//localhost:" + port;
        Driver driver = GremlinDatabase.driver(uri, config);
        // freshReadmeSnippet: createConfiguration

        // freshReadmeSnippet: useDriver
        try (Session session = driver.session()) {
            StatementResult result = session.run("CREATE (a:Greeting) " +
                    "SET a.message = $message " +
                    "RETURN a.message",
                parameters("message", "Hello"));

            String message = result.single().get(0).asString();

            assertThat(message).isEqualTo("Hello");
        }
        // freshReadmeSnippet: useDriver
    }

    @Test
    public void inMemory() throws Exception {
        // freshReadmeSnippet: inMemory
        TinkerGraph graph = TinkerFactory.createModern();
        GraphTraversalSource traversal = graph.traversal();
        Driver driver = GremlinDatabase.driver(traversal);
        // freshReadmeSnippet: inMemory

        try (Session session = driver.session()) {
            StatementResult result = session.run("RETURN 'Hello'");
            String message = result.single().get(0).asString();
            assertThat(message).isEqualTo("Hello");
        }
    }

    @Test
    public void ignoreIds() throws ExecutionException, InterruptedException {
        gremlinServer.gremlinClient().submit("g.addV('stringId1').property(id, 'string1')").all().get();
        gremlinServer.gremlinClient().submit("g.addV('stringId1').property(id, 'string2')").all().get();

        // freshReadmeSnippet: ignoreIds
        Config config = Config.build()
            .ignoreIds()
            .toConfig();
        // freshReadmeSnippet: ignoreIds

        Driver driver = GremlinDatabase.driver("//localhost:" + gremlinServer.getPort(), config);

        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (n:stringId1) RETURN n");
            assertThat(result.list())
                .extracting(r -> r.get("n").asNode().id())
                .containsExactly(-1L, -1L);
        }
    }


    @Test
    public void originalIds() throws ExecutionException, InterruptedException {
        String uri = "//localhost:" + gremlinServer.getPort();
        String uuid = "ef8b80c9-f8f9-40b6-bad2-ee4757d5bb33";

        gremlinServer.gremlinClient().submit(format("g.addV('VertexWithStringId').property(id, '%s')", uuid)).all().get();

        // freshReadmeSnippet: originalIds
        Config config = Config.build()
            .withTranslation(TranslatorFlavor.gremlinServer())
            .ignoreIds()
            .toConfig();

        Driver driver = GremlinDatabase.driver(uri, config);

        try (Session session = driver.session()) {
            StatementResult getOriginal = session.run("MATCH (n:VertexWithStringId) RETURN id(n) as id");
            Object originalId = getOriginal.single().get("id").asObject();
            assertThat(originalId).isEqualTo(uuid); // ef8b80c9-f8f9-40b6-bad2-ee4757d5bb33

            StatementResult result = session.run("MATCH (n) WHERE id(n) = $originalId RETURN n", singletonMap("originalId", originalId));
            Node n = result.single().get("n").asNode();

            assertThat(n.id()).isEqualTo(-1); // -1
        }
        // freshReadmeSnippet: originalIds
    }
}
