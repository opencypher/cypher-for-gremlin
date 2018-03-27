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
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

final class BytecodeCypherGremlinClient implements CypherGremlinClient {

    private final Client client;
    private final TranslatorFlavor<GraphTraversal, P> flavor;

    BytecodeCypherGremlinClient(Client client, TranslatorFlavor<GraphTraversal, P> flavor) {
        this.client = client;
        this.flavor = flavor;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters) {
        CypherAstWrapper ast;
        try {
            ast = CypherAstWrapper.parse(cypher, parameters);
        } catch (Exception e) {
            return completedFuture(exceptional(e));
        }

        if (ast.getOptions().contains(EXPLAIN)) {
            return completedFuture(explain(ast));
        }

        Translator<GraphTraversal, P> translator = Translator.builder().traversal().build(flavor);
        Bytecode bytecode = ast.buildTranslation(translator).asAdmin().getBytecode();
        CompletableFuture<ResultSet> resultSetFuture = client.submitAsync(bytecode);
        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());

        return resultSetFuture
            .thenApply(ResultSet::iterator)
            .thenApply(resultIterator -> new CypherResultSet(resultIterator, returnNormalizer::normalize));
    }
}
