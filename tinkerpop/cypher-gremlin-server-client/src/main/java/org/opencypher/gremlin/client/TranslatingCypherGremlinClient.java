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
import org.opencypher.gremlin.translation.TranslationFacade;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class TranslatingCypherGremlinClient implements CypherGremlinClient {

    private final Client client;
    private final TranslationFacade translationFacade;

    TranslatingCypherGremlinClient(Client client) {
        this.client = client;
        translationFacade = new TranslationFacade();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public CompletableFuture<List<Map<String, Object>>> submitAsync(String cypher, Map<String, Object> parameters) {
        String gremlin = translationFacade.toGremlin(cypher, parameters);
        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(gremlin, parameters);
        return ResultSetTransformer.resultSetAsMapAsync(resultSetFuture);
    }
}
