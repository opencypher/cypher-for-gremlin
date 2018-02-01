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

import static java.util.stream.Collectors.toList;
import static org.opencypher.gremlin.server.op.cypher.PluginCommunication.createRequest;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;

public class CypherGremlinClient implements Closeable {
    protected static final int DEFAULT_TIMEOUT_SEC = 5;
    protected Client client;

    public CypherGremlinClient(Supplier<Client> clientSupplier) {
        client = clientSupplier.get();
    }

    @Override
    public void close() {
        client.close();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> submitGremlin(String gremlin) {
        try {
            List<Result> results = client.submit(gremlin).all().get();
            return results.stream()
                .map(result -> (Map<String, Object>) result.get(Map.class))
                .collect(toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> submitCypher(String cypher) {
        return submitCypher(cypher, null);
    }

    public List<Map<String, Object>> submitCypher(String cypher, String graph) {
        try {
            return submitCypher(cypher, new HashMap<>(), graph, DEFAULT_TIMEOUT_SEC);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> submitCypher(String cypher, Map<String, Object> parameters, String graph, int timeOutSeconds) throws TimeoutException {
        try {
            List<Result> results = client.submitAsync(createRequest(cypher, parameters, graph)).get(timeOutSeconds, TimeUnit.SECONDS).all().get(timeOutSeconds, TimeUnit.SECONDS);
            return results.stream()
                .map(result -> (Map<String, Object>) result.get(Map.class))
                .collect(toList());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
