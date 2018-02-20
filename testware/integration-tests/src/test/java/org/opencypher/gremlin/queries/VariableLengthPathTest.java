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
package org.opencypher.gremlin.queries;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.util.Lists.newArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class VariableLengthPathTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    @Ignore("TinkerPop 3.3.0 migration - breaking change in SelectStep")
    public void variableLengthPath() throws Exception {
        String cypher = "MATCH (p:person {name: 'marko'}) " +
            "MATCH (p)-[r*1..2]->(s:software) " +
            "RETURN r, s.name AS software";
        List<Map<String, Object>> results = submitVariableLengthQuery(cypher);

        assertThat(results)
            .hasSize(3)
            .extracting("r", "software")
            .containsExactlyInAnyOrder(
                tuple(newArrayList("created"), "lop"),
                tuple(newArrayList("knows", "created"), "lop"),
                tuple(newArrayList("knows", "created"), "ripple")
            );
    }

    @Test
    public void fixedLengthPath() throws Exception {
        String cypher = "MATCH (:person {name: 'josh'})-[r*1..1]->(s:software) " +
            "RETURN r, s.name AS software";
        List<Map<String, Object>> results = submitVariableLengthQuery(cypher);

        assertThat(results)
            .hasSize(2)
            .extracting("r", "software")
            .containsExactlyInAnyOrder(
                tuple(newArrayList("created"), "lop"),
                tuple(newArrayList("created"), "ripple")
            );
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> submitVariableLengthQuery(String cypher) throws ExecutionException, InterruptedException {
        return submitAndGet(cypher).stream()
            .map(result -> {
                if (result.get("r") instanceof Edge) {
                    result.put("r", Arrays.asList(result.get("r")));
                }

                Map<String, Object> map = new HashMap<>();
                map.put("r", ((Collection<Edge>) result.get("r")).stream()
                    .map(Element::label)
                    .collect(toList()));

                map.put("software", result.get("software"));
                return map;
            })
            .collect(toList());
    }
}
