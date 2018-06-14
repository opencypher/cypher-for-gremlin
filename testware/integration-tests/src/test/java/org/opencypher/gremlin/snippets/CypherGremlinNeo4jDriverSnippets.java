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
import static org.neo4j.driver.v1.Values.parameters;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.opencypher.gremlin.neo4j.driver.Config;
import org.opencypher.gremlin.neo4j.driver.GremlinDatabase;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class CypherGremlinNeo4jDriverSnippets {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

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
}
