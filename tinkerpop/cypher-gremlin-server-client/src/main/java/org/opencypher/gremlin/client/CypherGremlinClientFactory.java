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
import org.opencypher.gremlin.translation.Flavor;

/**
 * This factory creates {@link CypherGremlinClient} instances of different kind.
 */
public class CypherGremlinClientFactory {
    private CypherGremlinClientFactory() {
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
        return new TranslatingCypherGremlinClient(client, Flavor.GREMLIN);
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
    public static CypherGremlinClient translating(Client client, Flavor flavor) {
        return new TranslatingCypherGremlinClient(client, flavor);
    }
}
