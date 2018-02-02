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
package org.opencypher.gremlin.server.op.cypher;

import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;

import java.util.HashMap;
import java.util.Map;

public final class PluginCommunication {

    private static final Map<String, Object> EMPTY_MAP = new HashMap<>();

    static final String OP_PROCESSOR_NAME = "cypher";

    static final String GRAPH_NAME = "graph";

    private PluginCommunication() {
    }

    public static RequestMessage createRequest(String cypher) {
        return buildRequest(cypher, EMPTY_MAP, null).create();
    }

    public static RequestMessage createRequest(String cypher, String graph) {
        return buildRequest(cypher, EMPTY_MAP, graph).create();
    }

    public static RequestMessage createRequest(String cypher, Map<String, Object> parameters) {
        return buildRequest(cypher, parameters, null).create();
    }

    public static RequestMessage createRequest(String cypher, Map<String, Object> parameters, String graph) {
        return buildRequest(cypher, parameters, graph).create();
    }

    public static RequestMessage.Builder buildRequest(String cypher) {
        return buildRequest(cypher, EMPTY_MAP, null);
    }

    public static RequestMessage.Builder buildRequest(String cypher, Map<String, Object> parameters, String graph) {
        RequestMessage.Builder request = RequestMessage.build(Tokens.OPS_EVAL)
            .processor(OP_PROCESSOR_NAME)
            .add(Tokens.ARGS_GREMLIN, cypher);

        // optional query parameters
        if (!parameters.isEmpty()) {
            request.addArg(Tokens.ARGS_BINDINGS, parameters);
        }

        // optional graph name
        if (graph != null) {
            request.addArg(GRAPH_NAME, graph);
        }
        return request;
    }
}
