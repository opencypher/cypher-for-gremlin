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
package org.opencypher.gremlin.server.performance.infra;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.openjdk.jmh.infra.Blackhole;

public class InMemoryClient implements CypherClient {

    private final Blackhole blackhole;
    private final CypherGremlinClient client;

    public InMemoryClient(Blackhole blackhole) {
        this.blackhole = blackhole;
        GraphTraversalSource traversal = TinkerGraph.open().traversal();
        client = CypherGremlinClient.inMemory(traversal);
    }

    @Override
    public void run(String cypher) {
        client.submit(cypher).forEach(blackhole::consume);
    }

    @Override
    public void close() {
    }
}
