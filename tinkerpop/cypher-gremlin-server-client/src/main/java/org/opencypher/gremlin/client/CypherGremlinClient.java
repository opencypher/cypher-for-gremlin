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
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.opencypher.gremlin.ClientServerCommunication.buildRequest;

/**
 * This is a convenience wrapper around a Gremlin {@link Client}
 * that configures the provided {@link Client} to be able to send Cypher queries
 * to a Gremlin Server with the Cypher to Gremlin plugin.
 */
public final class CypherGremlinClient implements Closeable {

    private final Client client;

    public CypherGremlinClient(Client client) {
        this.client = client;
    }

    /**
     * Closes the underlying Gremlin client.
     */
    @Override
    public void close() {
        client.close();
    }

    /**
     * Submits a Cypher query.
     *
     * @param cypher query text
     * @return Cypher-style results
     */
    public List<Map<String, Object>> submit(String cypher) {
        return submitAsync(cypher, emptyMap(), null).join();
    }

    /**
     * Submits a Cypher query.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @return Cypher-style results
     */
    public List<Map<String, Object>> submit(String cypher, Map<String, Object> parameters) {
        return submitAsync(cypher, parameters, null).join();
    }

    /**
     * Submits a Cypher query.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @param graphName  target graph name
     * @return Cypher-style results
     * @see org.opencypher.gremlin.ClientServerCommunication
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> submit(String cypher, Map<String, Object> parameters, String graphName) {
        return submitAsync(cypher, parameters, graphName).join();
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher query text
     * @return Cypher-style results
     */
    public CompletableFuture<List<Map<String, Object>>> submitAsync(String cypher) {
        return submitAsync(cypher, emptyMap(), null);
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @return Cypher-style results
     */
    public CompletableFuture<List<Map<String, Object>>> submitAsync(String cypher, Map<String, Object> parameters) {
        return submitAsync(cypher, parameters, null);
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @param graphName  target graph name
     * @return Cypher-style results
     * @see org.opencypher.gremlin.ClientServerCommunication
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Map<String, Object>>> submitAsync(
        String cypher,
        Map<String, Object> parameters,
        String graphName
    ) {
        RequestMessage requestMessage = buildRequest(cypher, parameters, graphName).create();
        return client.submitAsync(requestMessage)
            .thenCompose(ResultSet::all)
            .thenApply(results -> results.stream()
                .map(result -> (Map<String, Object>) result.get(Map.class))
                .map(ReturnNormalizer::normalize)
                .collect(toList()));
    }
}
