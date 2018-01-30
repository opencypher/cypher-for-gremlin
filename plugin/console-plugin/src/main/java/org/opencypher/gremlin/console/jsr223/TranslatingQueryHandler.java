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
package org.opencypher.gremlin.console.jsr223;

import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.Flavor;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.opencypher.gremlin.traversal.GremlinRemote.normalizeRow;

/**
 * Translates Cypher query to Gremlin query and builds request to plugin-less Gremlin Server.
 */
public class TranslatingQueryHandler implements QueryHandler {

    final Flavor flavor;

    TranslatingQueryHandler(Flavor flavor) {
        this.flavor = flavor;
    }

    @Override
    public RequestMessage.Builder buildRequest(String query) {
        String gremlin = cypherToGremlin(query);
        return RequestMessage.build(Tokens.OPS_EVAL)
            .add(Tokens.ARGS_GREMLIN, gremlin);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Result> normalizeResults(List<Result> results) {
        if (!results.isEmpty() && !(results.get(0).getObject() instanceof Map)) {
            throw new RuntimeException(
                "Unable to parse result rows. " +
                    "Please make sure that 'serializeResultToString' option is disabled in remote configuration"
            );
        }

        return results.stream()
            .map(result -> {
                Map<String, Object> normalized = normalizeRow(result.get(Map.class));
                return new Result(normalized);
            })
            .collect(toList());
    }

    private String cypherToGremlin(String cypher) {
        return CypherAstWrapper.parse(cypher)
            .buildTranslation(flavor.getTranslator());
    }
}
