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
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

final class GroovyCypherGremlinClient implements CypherGremlinClient {

    private final Client client;
    private final TranslatorFlavor<String, GroovyPredicate> flavor;

    GroovyCypherGremlinClient(Client client, TranslatorFlavor<String, GroovyPredicate> flavor) {
        this.client = client;
        this.flavor = flavor;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters) {
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, parameters);
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build(flavor);
        String gremlin = ast.buildTranslation(translator);
        Map<String, Object> extractedParameters = ast.getExtractedParameters();
        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(gremlin, extractedParameters);
        return resultSetFuture
            .thenApply(ResultSet::iterator)
            .thenApply(CypherResultSet::new);
    }
}
