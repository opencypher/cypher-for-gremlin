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

import static org.opencypher.gremlin.translation.translator.TranslatorFeature.DONT_FINALIZE_ELEMENTS;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.opencypher.gremlin.translation.CypherAst;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.ParameterNormalizer;

public class CypherTraversalSource extends GraphTraversalSource {
    public CypherTraversalSource(Graph graph, TraversalStrategies traversalStrategies) {
        super(graph, traversalStrategies);
    }

    public CypherTraversalSource(Graph graph) {
        super(graph);
    }

    public GraphTraversal<Map<String, Object>, Map<String, Object>> cypher(final String cypher) {
        return cypher(cypher, Collections.emptyMap(), "");
    }

    public GraphTraversal<Map<String, Object>, Map<String, Object>> cypher(final String cypher, String flavor) {
        return cypher(cypher, Collections.emptyMap(), flavor);
    }

    public GraphTraversal<Map<String, Object>, Map<String, Object>> cypher(final String cypher, final Map<String, Object> params) {
        return cypher(cypher, params, "");
    }

    @SuppressWarnings("unchecked")
    public GraphTraversal<Map<String, Object>, Map<String, Object>> cypher(final String cypher, final Map<String, Object> params, String flavor) {
        DefaultGraphTraversal g = new DefaultGraphTraversal(this.clone());
        Map<String, Object> parameters = ParameterNormalizer.normalize(params);
        CypherAst ast = CypherAst.parse(cypher, parameters);

        Translator<GraphTraversal, P> traversalTranslator = Translator.builder()
            .traversal(g)
            .enable(DONT_FINALIZE_ELEMENTS)
            .build(flavor);

        GraphTraversal traversal = ast.buildTranslation(traversalTranslator);

        return traversal;
    }

    @Override
    public CypherTraversalSource withRemote(final Configuration conf) {
        return (CypherTraversalSource) super.withRemote(conf);
    }

    @Override
    public CypherTraversalSource withRemote(final String configFile) throws Exception {
        return (CypherTraversalSource) super.withRemote(configFile);
    }

    @Override
    public CypherTraversalSource withRemote(final RemoteConnection connection) {
        return (CypherTraversalSource) super.withRemote(connection);
    }

}
