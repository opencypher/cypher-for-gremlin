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

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.opencypher.gremlin.client.CommonResultSets.exceptional;
import static org.opencypher.gremlin.client.CommonResultSets.explain;
import static org.opencypher.gremlin.translation.StatementOption.EXPLAIN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
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
    public CypherResultSet submit(String cypher) {
        return submit(cypher, new HashMap<>());
    }

    @Override
    public CypherResultSet submit(String cypher, Map<String, ?> parameters) {
        Map<String, Object> normalizedParameters = ParameterNormalizer.normalize(parameters);
        CypherAst ast = CypherAst.parse(cypher, normalizedParameters);

        if (ast.getOptions().contains(EXPLAIN)) {
            return explain(ast);
        }

        Translator<String, GroovyPredicate> translator = translatorSupplier.get();
        String gremlin = ast.buildTranslation(translator);

        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());

        try {
            List<Result> resultSet = client.submit(gremlin, normalizedParameters).all().get();
            return new CypherResultSet(resultSet.iterator(), returnNormalizer::normalize);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters) {
        Map<String, Object> normalizedParameters = ParameterNormalizer.normalize(parameters);
        CypherAst ast;
        try {
            ast = CypherAst.parse(cypher, normalizedParameters);
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

        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(gremlin, normalizedParameters);
        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());
        return resultSetFuture
            .thenApply(ResultSet::iterator)
            .thenApply(resultIterator -> new CypherResultSet(resultIterator, returnNormalizer::normalize));
    }
}
