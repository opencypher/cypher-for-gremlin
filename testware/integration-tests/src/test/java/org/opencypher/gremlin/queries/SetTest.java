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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithBytecode;
import org.opencypher.gremlin.groups.SkipWithGremlinGroovy;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class SetTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Before
    public void setUp() {
        gremlinServer.gremlinClient().submit("g.V().drop()").all().join();
        gremlinServer.gremlinClient().submit("g.addV()").all().join();
    }

    @Test
    public void setAndGetString() throws Exception {
        assertThat(setAndGetProperty("1")).containsExactly(1L);
    }

    @Test
    public void setAndGetQuotedString() throws Exception {
        assertThat(setAndGetProperty("'123'")).containsExactly("123");
    }

    @Test
    public void setAndGetEmptyList() throws Exception {
        assertThat(setAndGetProperty("[]")).containsExactly((Object) null);
    }

    @Test
    public void setAndGetList() throws Exception {
        assertThat(setAndGetProperty("[1, 2, 3]")).containsExactly(asList(1L, 2L, 3L));
    }

    /**
     * Maps don't work in client-side translations
     */
    @Test
    @Category({
        SkipWithGremlinGroovy.class,
        SkipWithBytecode.class
    })
    public void setAndGetMap() throws Exception {
        assertThat(setAndGetProperty("{key: 'value'}")).containsExactly(singletonMap("key", "value"));
    }

    private List<Object> setAndGetProperty(String value) throws Exception {
        String query = "MATCH (n) SET n.property = %s RETURN n.property AS prop LIMIT 1";
        return submitAndGet(format(query, value)).stream().map(r -> r.get("prop")).collect(toList());
    }

    @Test
    public void removeVertexProperty() {
        submitAndGet("CREATE (n:A {propertyToDelete: 'prop'})");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:A) SET n.property1 = null RETURN n.propertyToDelete");

        assertThat(update)
            .extracting("n.name")
            .hasSize(1)
            .containsNull();
    }

    @Test
    public void removeVertexProperty2() {
        submitAndGet("CREATE (n:A {property1: 'prop', property2: 'prop', property3: 'prop'})");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:A) REMOVE n.property1, n.property2 RETURN n.property1, n.property2, n.property3");

        assertThat(update)
            .extracting("n.property1", "n.property2", "n.property3")
            .containsExactly(tuple(null, null, "prop"));
    }

    @Test
    public void setEdgeProperty() {
        submitAndGet("CREATE ()-[:REL]->()");

        List<Map<String, Object>> update = submitAndGet("MATCH (n)-[r]->(m) SET r.property1 = 'value1'\n" +
            "RETURN r.property1");

        assertThat(update)
            .extracting("r.property1")
            .containsExactly("value1");
    }

    @Test
    public void setEdgeProperty2() {
        submitAndGet("CREATE (n)-[r:REL]->(m)");

        List<Map<String, Object>> update = submitAndGet("MATCH (n)-[r:REL]->(m) SET (r).name = 'neo4j'\n" +
            "RETURN r.name");

        assertThat(update)
            .extracting("r.name")
            .containsExactly("neo4j");
    }

    @Test
    public void unsetEdgeProperty() {
        submitAndGet("CREATE (n)-[r:REL {property1: 'prop'}]->(m)");

        List<Map<String, Object>> update = submitAndGet("MATCH (n)-[r]->(m) SET r.property1 = null\n" +
            "RETURN r.property1");

        assertThat(update)
            .extracting("r.property1")
            .hasSize(1)
            .containsNull();
    }

    @Test
    public void addPropertiesWithMap() {
        submitAndGet("CREATE (n:person {loc: 'uk'})");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:person) SET n += {name: 'marko', age: 28}" +
            "\nRETURN n.name, n.age, n.loc");

        assertThat(update)
            .extracting("n.name", "n.age", "n.loc")
            .containsExactly(tuple("marko", 28L, "uk"));
    }

    @Test
    public void setPropertiesWithMap() {
        submitAndGet("CREATE (n:person {name: 'peter', age: 60, loc: 'uk'})");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:person) SET n += {name: 'marko', age: 28}\n" +
            "RETURN n.name, n.age, n.loc");

        assertThat(update)
            .extracting("n.name", "n.age", "n.loc")
            .containsExactly(tuple("marko", 28L, "uk"));
    }
}
