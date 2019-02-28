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
package org.opencypher.gremlin.neo4j.driver;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

public class GremlinGraphDriverTest {

    @Test
    public void tinkerGraph() {
        TinkerGraph tinkerGraph = TinkerFactory.createModern();
        GraphTraversalSource graphTraversalSource = tinkerGraph.traversal();
        GremlinGraphDriver driver = new GremlinGraphDriver(graphTraversalSource);

        List<Map<String, Object>> results;
        try (Session session = driver.session()) {
            StatementResult result = session.run(
                "MATCH (n:person) " +
                    "WHERE n.age = $age " +
                    "RETURN n.name",
                parameters("age", 29)
            );

            results = result.list().stream()
                .map(Record::asMap)
                .collect(toList());
        }

        assertThat(results)
            .extracting("n.name")
            .containsExactly("marko");
    }
}
