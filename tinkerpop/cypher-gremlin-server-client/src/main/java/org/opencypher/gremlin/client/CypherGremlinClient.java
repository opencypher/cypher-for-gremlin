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

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyMap;

/**
 * This is a convenience wrapper around a Gremlin {@link Client}
 * that configures the provided {@link Client} to be able to send Cypher queries
 * to a Gremlin Server.
 */
public interface CypherGremlinClient extends Closeable {
    /**
     * Closes the underlying Gremlin client.
     */
    @Override
    void close();

    /**
     * Submits a Cypher query.
     *
     * @param cypher query text
     * @return Cypher-style results
     */
    default List<Map<String, Object>> submit(String cypher) {
        return submitAsync(cypher, emptyMap()).join();
    }

    /**
     * Submits a Cypher query.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @return Cypher-style results
     */
    default List<Map<String, Object>> submit(String cypher, Map<String, Object> parameters) {
        return submitAsync(cypher, parameters).join();
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher query text
     * @return Cypher-style results
     */
    default CompletableFuture<List<Map<String, Object>>> submitAsync(String cypher) {
        return submitAsync(cypher, emptyMap());
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @return Cypher-style results
     * @see org.opencypher.gremlin.ClientServerCommunication
     */
    CompletableFuture<List<Map<String, Object>>> submitAsync(String cypher, Map<String, Object> parameters);
}
