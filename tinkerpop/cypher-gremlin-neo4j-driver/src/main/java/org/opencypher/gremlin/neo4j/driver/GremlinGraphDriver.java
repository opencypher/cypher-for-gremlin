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
package org.opencypher.gremlin.neo4j.driver;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.opencypher.gremlin.client.CypherGremlinClient;

public class GremlinGraphDriver implements GremlinDriver {
    private final GraphTraversalSource graphTraversalSource;
    private final GremlinServerInfo serverInfo;

    GremlinGraphDriver(GraphTraversalSource graphTraversalSource) {
        this.graphTraversalSource = graphTraversalSource;
        serverInfo = new GremlinServerInfo();
    }

    @Override
    public boolean isEncrypted() {
        return false;
    }

    @Override
    public Session session() {
        CypherGremlinClient cypherGremlinClient = CypherGremlinClient.inMemory(graphTraversalSource);
        return new GremlinServerSession(serverInfo, cypherGremlinClient);
    }

    @Override
    public void close() {
    }

    static class GremlinServerInfo implements ServerInfo {
        @Override
        public String address() {
            throw new UnsupportedOperationException("In-memory graph does not have a network address");
        }

        @Override
        public String version() {
            return null;
        }
    }
}
