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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class ParameterTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameters).all();
    }

    @Test
    public void whereEquals() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) " +
                "WHERE n.name = $name " +
                "RETURN n.age AS age",
            singletonMap("name", "marko")
        );

        assertThat(results)
            .extracting("age")
            .containsExactly(29L);
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void whereIn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) " +
                "WHERE n.name IN $names " +
                "RETURN n.age AS age",
            singletonMap("names", asList("marko", "vadas"))
        );

        assertThat(results)
            .extracting("age")
            .containsExactlyInAnyOrder(27L, 29L);
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void patternMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person {name: $name}) " +
                "RETURN n.age AS age",
            singletonMap("name", "marko")
        );

        assertThat(results)
            .extracting("age")
            .containsExactly(29L);
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void startsWith() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) " +
                "WHERE n.name STARTS WITH $prefix " +
                "RETURN n.name AS name",
            singletonMap("prefix", "ma")
        );

        assertThat(results)
            .extracting("name")
            .containsExactly("marko");
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void containerIndex() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH ['Apa'] AS expr " +
                "RETURN expr[$idx] AS value",
            singletonMap("idx", 0)
        );

        assertThat(results)
            .extracting("value")
            .containsExactly("Apa");
    }

    @Test
    public void unwind() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND $numbers AS arr " +
                "RETURN arr",
            singletonMap("numbers", asList(1L, 2L, 3L))
        );

        assertThat(results)
            .extracting("arr")
            .containsExactly(1L, 2L, 3L);
    }
}
