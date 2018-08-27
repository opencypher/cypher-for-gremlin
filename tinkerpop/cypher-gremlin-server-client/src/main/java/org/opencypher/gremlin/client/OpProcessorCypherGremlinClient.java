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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;

final class OpProcessorCypherGremlinClient implements CypherGremlinClient {

    private static final String CYPHER_OP_PROCESSOR_NAME = "cypher";

    private final Client client;

    OpProcessorCypherGremlinClient(Client client) {
        this.client = client;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters) {
        RequestMessage requestMessage = buildRequest(cypher, parameters).create();
        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(requestMessage);

        return resultSetFuture
            .thenApply(ResultSet::iterator)
            .thenApply(CypherResultSet::new);
    }

    private static RequestMessage.Builder buildRequest(String query, Map<String, ?> parameters) {
        RequestMessage.Builder request = RequestMessage.build(Tokens.OPS_EVAL)
            .processor(CYPHER_OP_PROCESSOR_NAME)
            .add(Tokens.ARGS_GREMLIN, query);

        if (parameters != null && !parameters.isEmpty()) {
            request.addArg(Tokens.ARGS_BINDINGS, parameters);
        }

        return request;
    }
}
