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

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

/**
 * This factory creates {@link CypherGremlinClient} instances of different kind.
 */
public class CypherGremlinClients {
    private CypherGremlinClients() {
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to a remote Gremlin Server with Cypher plugin.
     *
     * @param client Gremlin client
     * @return Cypher-enabled client
     */
    public static CypherGremlinClient plugin(Client client) {
        return new OpProcessorCypherGremlinClient(client);
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client Gremlin client
     * @return Cypher-enabled client
     */
    public static CypherGremlinClient translating(Client client) {
        return new TranslatingCypherGremlinClient(client, TranslatorFlavor.gremlinServer());
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client Gremlin client
     * @param flavor translation flavor
     * @return Cypher-enabled client
     */
    public static CypherGremlinClient translating(Client client, TranslatorFlavor<String, GroovyPredicate> flavor) {
        return new TranslatingCypherGremlinClient(client, flavor);
    }

    /**
     * Creates a {@link CypherGremlinClient} that executes Cypher queries
     * directly on the configured {@link GraphTraversalSource}.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread.
     * Graph traversal execution is not synchronized.
     *
     * @param gts source of {@link GraphTraversal} to translate to
     * @return Cypher-enabled client
     */
    public static CypherGremlinClient inMemory(GraphTraversalSource gts) {
        return new InMemoryCypherGremlinClient(gts);
    }
}
