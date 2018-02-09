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
package org.opencypher.gremlin.rules;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.junit.rules.ExternalResource;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.client.CypherGremlinClients;
import org.opencypher.gremlin.client.GremlinClientFactory;
import org.opencypher.gremlin.server.EmbeddedGremlinServer;

import static org.opencypher.gremlin.server.EmbeddedGremlinServerFactory.tinkerGraph;

public class GremlinServerExternalResource extends ExternalResource {

    private EmbeddedGremlinServer gremlinServer;
    private Client gremlinClient;
    private CypherGremlinClient cypherGremlinClient;

    @Override
    public void before() throws Throwable {
        gremlinServer = tinkerGraph();
        gremlinServer.start();
        int port = gremlinServer.getPort();
        gremlinClient = GremlinClientFactory.create(port);
        cypherGremlinClient = CypherGremlinClients.plugin(gremlinClient);
    }

    @Override
    public void after() {
        cypherGremlinClient.close();
        gremlinServer.stop();
    }

    public int getPort() {
        return gremlinServer.getPort();
    }

    public Client gremlinClient() {
        return gremlinClient;
    }

    public CypherGremlinClient cypherGremlinClient() {
        return cypherGremlinClient;
    }
}
