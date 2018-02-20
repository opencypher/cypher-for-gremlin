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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class MergeTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Before
    public void setUp() {
        gremlinServer.gremlinClient().submit("g.V().drop()").all().join();
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void mergeVertex() throws Exception {
        String query = "MERGE (a:Label {prop: 'value'}) " +
            "ON MATCH SET a.action = 'on match' " +
            "ON CREATE SET a.action = 'on create' " +
            "RETURN a.action, a.prop, labels(a)";

        // checking created vertex properties
        List<Map<String, Object>> results = submitAndGet(query);
        assertThat(results)
            .extracting("a.prop", "labels(a)")
            .containsExactly(tuple("value", singletonList("Label")));

        // checking SET clause
        assertThat(results)
            .extracting("a.action")
            .containsExactly("on create");

        // executing the same query - SET clause result is different
        results = submitAndGet(query);
        assertThat(results)
            .extracting("a.action")
            .containsExactly("on match");
    }

    @Test
    public void createRelationshipWhenAllMatchesFilteredOut() throws Exception {
        submitAndGet("CREATE (a:A), (b:B)");

        String query = "MATCH (a:A), (b:B) " +
            "MERGE (a)-[r:TYPE {name: 'r2'}]->(b) " +
            "RETURN count(r) AS count";

        // checking that relation alias is exported from CREATE
        List<Map<String, Object>> results = submitAndGet(query);
        assertThat(results)
            .extracting("count")
            .containsExactly(1L);

        // checking that relation alias is exported from MATCH
        results = submitAndGet(query);
        assertThat(results)
            .extracting("count")
            .containsExactly(1L);
    }

}
