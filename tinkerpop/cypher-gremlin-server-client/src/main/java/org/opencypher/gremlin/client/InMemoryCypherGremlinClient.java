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
import static java.util.stream.Collectors.toList;
import static org.opencypher.gremlin.translation.StatementOption.EXPLAIN;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

final class InMemoryCypherGremlinClient implements CypherGremlinClient {

    private final GraphTraversalSource gts;

    InMemoryCypherGremlinClient(GraphTraversalSource gts) {
        this.gts = gts;
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters) {
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, parameters);

        if (ast.getOptions().contains(EXPLAIN)) {
            Map<String, Object> explanation = new LinkedHashMap<>();
            explanation.put("translation", ast.buildTranslation(Translator.builder().gremlinGroovy().build()));
            explanation.put("options", ast.getOptions().toString());
            List<Result> results = Collections.singletonList(new Result(explanation));
            return completedFuture(new CypherResultSet(results.iterator()));
        }

        DefaultGraphTraversal g = new DefaultGraphTraversal(gts.clone());
        GraphTraversal<?, ?> traversal = ast.buildTranslation(Translator.builder().traversal(g).build());
        List<Result> results = traversal.toStream()
            .map(ReturnNormalizer::normalize)
            .map(Result::new)
            .collect(toList());
        return completedFuture(new CypherResultSet(results.iterator()));
    }
}
