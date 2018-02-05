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
package org.opencypher.gremlin;

import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;

import java.util.Map;

import static java.util.Collections.emptyMap;

public final class ClientServerCommunication {
    public static final String CYPHER_OP_PROCESSOR_NAME = "cypher";
    public static final String DEFAULT_GRAPH_NAME = "graph";

    private ClientServerCommunication() {
    }

    public static RequestMessage.Builder buildRequest(String cypher) {
        return buildRequest(cypher, emptyMap(), null, CYPHER_OP_PROCESSOR_NAME);
    }

    public static RequestMessage.Builder buildRequest(String cypher, Map<String, Object> parameters) {
        return buildRequest(cypher, parameters, null, CYPHER_OP_PROCESSOR_NAME);
    }

    public static RequestMessage.Builder buildRequest(
        String cypher,
        Map<String, Object> parameters,
        String graphName
    ) {
        return buildRequest(cypher, parameters, graphName, CYPHER_OP_PROCESSOR_NAME);
    }

    public static RequestMessage.Builder buildRequest(
        String query,
        Map<String, Object> parameters,
        String graphName,
        String opProcessor
    ) {
        RequestMessage.Builder request = RequestMessage.build(Tokens.OPS_EVAL)
            .processor(opProcessor)
            .add(Tokens.ARGS_GREMLIN, query);

        if (parameters != null && !parameters.isEmpty()) {
            request.addArg(Tokens.ARGS_BINDINGS, parameters);
        }

        if (graphName != null) {
            request.addArg(DEFAULT_GRAPH_NAME, graphName);
        }
        return request;
    }
}
