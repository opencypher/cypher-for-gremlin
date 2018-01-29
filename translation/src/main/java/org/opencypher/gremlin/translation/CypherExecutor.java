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
package org.opencypher.gremlin.translation;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.opencypher.gremlin.traversal.ReturnNormalizer.toCypherResults;

/**
 * Simple Cypher executor that is intended for in-memory Gremlin graphs.
 * <p>
 * Note: if you want to run Cypher queries against a Gremlin Server, use the Cypher for Gremlin plugin instead.
 */
public final class CypherExecutor {

    private final GraphTraversalSource gts;

    /**
     * Creates a new instance that will build {@link GraphTraversal} from the provided source.
     *
     * @param gts source of {@link GraphTraversal} to translate to
     */
    public CypherExecutor(GraphTraversalSource gts) {
        this.gts = gts;
    }

    /**
     * Executes a Cypher query on the configured {@link GraphTraversalSource}.
     *
     * @param cypher Cypher query
     * @return query result
     */
    public List<Map<String, Object>> execute(String cypher) {
        return execute(cypher, null);
    }

    /**
     * Executes a Cypher query on the configured {@link GraphTraversalSource}.
     *
     * @param cypher     Cypher query
     * @param parameters query parameters
     * @return query result
     */
    public List<Map<String, Object>> execute(String cypher, Map<String, Object> parameters) {
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, parameters);
        DefaultGraphTraversal g = new DefaultGraphTraversal(gts.clone());
        TranslationPlan<GraphTraversal> translationPlan = ast.buildTranslation(TranslatorFactory.traversal(g));

        if (translationPlan.hasOption(StatementOption.EXPLAIN)) {
            Map<String, Object> explanation = ast.buildTranslation(TranslatorFactory.string()).explain();
            return singletonList(explanation);
        }

        GraphTraversal<?, ?> translation = translationPlan.getTranslation();
        GraphTraversal<?, Map<String, Object>> iterator = translation.map(toCypherResults());
        return iterator.toList();
    }
}
