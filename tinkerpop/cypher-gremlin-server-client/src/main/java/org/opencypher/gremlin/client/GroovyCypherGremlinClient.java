/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.opencypher.gremlin.client.CommonResultSets.exceptional;
import static org.opencypher.gremlin.client.CommonResultSets.explain;
import static org.opencypher.gremlin.translation.StatementOption.EXPLAIN;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.opencypher.gremlin.translation.CypherAst;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.ParameterNormalizer;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

final class GroovyCypherGremlinClient implements CypherGremlinClient {

    private final Client client;
    private final Supplier<Translator<String, GroovyPredicate>> translatorSupplier;

    GroovyCypherGremlinClient(Client client, Supplier<Translator<String, GroovyPredicate>> translatorSupplier) {
        this.client = client;
        this.translatorSupplier = translatorSupplier;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(CypherStatement statement) {
        Map<String, Object> normalizedParameters = ParameterNormalizer.normalize(statement.parameters());
        CypherAst ast;
        try {
            ast = CypherAst.parse(statement.query(), normalizedParameters);
        } catch (Exception e) {
            return completedFuture(exceptional(e));
        }

        if (ast.getOptions().contains(EXPLAIN)) {
            return completedFuture(explain(ast));
        }

        Translator<String, GroovyPredicate> translator = translatorSupplier.get();
        String gremlin;
        try {
            gremlin = ast.buildTranslation(translator);
        } catch (Exception e) {
            return completedFuture(exceptional(e));
        }

        RequestMessage request = buildRequest(gremlin, normalizedParameters, statement).create();

        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(request);
        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());
        return resultSetFuture
            .thenApply(ResultSet::iterator)
            .thenApply(resultIterator -> new CypherResultSet(resultIterator, returnNormalizer::normalize));
    }

    private static RequestMessage.Builder buildRequest(String query, Map<String, Object> normalizedParameters, CypherStatement statement) {
        RequestMessage.Builder request = RequestMessage.build(Tokens.OPS_EVAL)
            .add(Tokens.ARGS_GREMLIN, query);

        statement.timeout().ifPresent(t -> request.add(Tokens.ARGS_SCRIPT_EVAL_TIMEOUT, t));

        if (!normalizedParameters.isEmpty()) {
            request.addArg(Tokens.ARGS_BINDINGS, normalizedParameters);
        }

        return request;
    }
}
