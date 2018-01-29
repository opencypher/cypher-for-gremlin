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
package org.opencypher.gremlin.server.performance.infra;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.Translator;
import org.opencypher.gremlin.translation.TranslatorFactory;
import org.openjdk.jmh.infra.Blackhole;

public class TranslationOnlyClient implements CypherClient {

    private final Blackhole blackhole;

    public TranslationOnlyClient(Blackhole blackhole) {
        this.blackhole = blackhole;
    }

    @Override
    public void run(String cypher) {
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher);
        DefaultGraphTraversal g = new DefaultGraphTraversal();
        Translator<GraphTraversal, P> translator = TranslatorFactory.traversal(g);
        blackhole.consume(ast.buildTranslation(translator));
    }

    @Override
    public void close() {
    }
}
