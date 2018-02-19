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
package org.opencypher.gremlin;

import static org.apache.tinkerpop.gremlin.structure.io.IoCore.gryo;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;

public class TinkerGraphTest {

    @Test
    public void people() throws Exception {
        TinkerGraph graph = TinkerFactory.createModern();
        GraphTraversalSource g = graph.traversal();

        GraphTraversal<Vertex, Vertex> result = g.V().has(T.label, "person");
        List<Vertex> vertices = result.toList();

        assertThat(vertices)
            .extracting(v -> v.property("name"))
            .extracting(Property::value)
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void names() throws Exception {
        TinkerGraph graph = TinkerFactory.createModern();
        GraphTraversalSource g = graph.traversal();

        GraphTraversal<Vertex, String> result = g.V().has(T.label, "person").values("name");

        assertThat(result.toList()).containsExactly("marko", "vadas", "josh", "peter");
    }

    @Test
    public void count() throws Exception {
        TinkerGraph graph = TinkerGraph.open();
        graph.io(gryo()).readGraph("src/test/resources/grateful-dead.kryo");
        GraphTraversalSource g = graph.traversal();

        Long result = g.V().count().next();

        assertThat(result).isEqualTo(808);
    }
}
