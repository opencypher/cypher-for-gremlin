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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.opencypher.gremlin.translation.CypherAst;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.ParameterNormalizer;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

final class BytecodeCypherGremlinClient implements CypherGremlinClient {

    private final Client client;
    private final Supplier<Translator<Bytecode, P>> translatorSupplier;

    BytecodeCypherGremlinClient(Client client, Supplier<Translator<Bytecode, P>> translatorSupplier) {
        this.client = client;
        this.translatorSupplier = translatorSupplier;
    }

    @Override
    public void close() {
        client.close();
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

        Translator<Bytecode, P> translator = translatorSupplier.get();
        Bytecode bytecode;
        try {
            bytecode = ast.buildTranslation(translator);
        } catch (Exception e) {
            return completedFuture(exceptional(e));
        }

        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(bytecode);
        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());
        return resultSetFuture
            .thenApply(ResultSet::iterator)
            .thenApply(resultIterator -> new CypherResultSet(
                new TraverserIterator(resultIterator),
                returnNormalizer::normalize
            ));
    }
}
