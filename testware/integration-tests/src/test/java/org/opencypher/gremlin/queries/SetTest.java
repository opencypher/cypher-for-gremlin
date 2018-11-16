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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;
import static org.opencypher.gremlin.test.TestCommons.parameterMap;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithJanusGraph;
import org.opencypher.gremlin.groups.UsesCollectionsInProperties;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class SetTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Object... parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameterMap(parameters)).all();
    }

    @Before
    public void setUp() {
        gremlinServer.gremlinClient().submit("g.V().drop()").all().join();
        gremlinServer.gremlinClient().submit("g.addV()").all().join();
    }

    @Test
    @Category(SkipWithJanusGraph.ChangePropertyType.class)
    public void setAndGetLiteral() throws Exception {
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
    @Category(UsesCollectionsInProperties.ListDataType.class)
    public void setAndGetList() throws Exception {
        assertThat(setAndGetProperty("[1, 2, 3]")).containsExactly(asList(1L, 2L, 3L));
    }

    @Test
    @Category(UsesCollectionsInProperties.MapDataType.class)
    public void setAndGetMap() throws Exception {
        assertThat(setAndGetProperty("{key: 'value'}")).containsExactly(singletonMap("key", "value"));
    }

    private List<Object> setAndGetProperty(String value) throws Exception {
        String query = "MATCH (n) SET n.property1 = %s RETURN n.property1 AS prop LIMIT 1";
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
    @Category(SkipWithJanusGraph.SetAndGetEdgeProperty.class)
    public void setEdgeProperty() {
        submitAndGet("CREATE ()-[:REL]->()");

        List<Map<String, Object>> update = submitAndGet("MATCH (n)-[r]->(m) SET r.property1 = 'value1'\n" +
            "RETURN r.property1");

        assertThat(update)
            .extracting("r.property1")
            .containsExactly("value1");
    }

    @Test
    @Category(SkipWithJanusGraph.SetAndGetEdgeProperty.class)
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
    public void setParameter() {
        submitAndGet("CREATE (n:person {loc: 'uk'})");

        String cypher = "MATCH (n:person)" +
            "SET n.name = {prop} " +
            "RETURN properties(n) as p";

        List<Map<String, Object>> update = submitAndGet(cypher, "prop", "george");

        System.out.println(update);

        assertThat(update)
            .extracting("p")
            .containsExactly(ImmutableMap.of(
                "loc", "uk",
                "name", "george"));
    }

    @Test
    public void addPropertiesWithMapParameter() {
        submitAndGet("CREATE (n:person {loc: 'uk'})");

        Map<Object, Object> props = new HashMap<>();
        props.put("name1", 1);
        props.put("name2", 2);

        String cypher = "MATCH (n:person)" +
            "SET n += {props} " +
            "RETURN properties(n) as p";

        List<Map<String, Object>> update = submitAndGet(cypher, "props", props);

        assertThat(update)
            .extracting("p")
            .containsExactly(ImmutableMap.of(
                "loc", "uk",
                "name1", 1L,
                "name2", 2L));
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

    @Test
    public void setPropertyToAnExpression() {
        submitAndGet("CREATE (:A {bar: 2})");
        submitAndGet("CREATE (:B {bar: 3})");

        List<Map<String, Object>> update = submitAndGet(
            "MATCH (a:A), (b:B) " +
                "SET a.bar = b.bar " +
                "RETURN a.bar"
        );

        assertThat(update)
            .extracting("a.bar")
            .containsExactly(3L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void copyPropertiesNodeToNode() {
        submitAndGet("CREATE (:FROM {prop1: 'a', prop2: 'b'})-[:REL]->(:TO {prop1: 'x', prop3: 'y'})");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:FROM)-[r]->(m:TO) SET m=n RETURN m");
        List<Map<String, Object>> result = submitAndGet("MATCH (m:TO) RETURN m");

        assertThat(update)
            .isEqualTo(result)
            .extracting("m")
            .extracting(byElementProperty("prop1", "prop2", "prop3"))
            .contains(tuple("a", "b", null));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void copyPropertiesNodeToRelationship() {
        submitAndGet("CREATE (:FROM {prop1: 'a', prop2: 'b'})-[:REL {prop1: 'x', prop3: 'y'}]->()");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:FROM)-[r]->(m) SET r=n RETURN r");
        List<Map<String, Object>> result = submitAndGet("MATCH (n)-[r:REL]->(m) RETURN r");

        assertThat(update)
            .isEqualTo(result)
            .extracting("r")
            .extracting(byElementProperty("prop1", "prop2", "prop3"))
            .contains(tuple("a", "b", null));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void copyPropertiesRelationshipToNode() {
        submitAndGet("CREATE (:TO {prop1: 'a', prop2: 'b'})-[:REL {prop1: 'x', prop3: 'y'}]->()");

        List<Map<String, Object>> update = submitAndGet("MATCH (n:TO)-[r]->(m) SET n=r RETURN n");
        List<Map<String, Object>> result = submitAndGet("MATCH (n:TO) RETURN n");

        assertThat(update)
            .isEqualTo(result)
            .extracting("n")
            .extracting(byElementProperty("prop1", "prop2", "prop3"))
            .contains(tuple("x", null, "y"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void copyPropertiesFromNull() {
        submitAndGet("CREATE (:TO {prop1: 'x', prop3: 'y'})");

        assertThatThrownBy(() -> submitAndGet("OPTIONAL MATCH (x:NOT_EXISTING) WITH x MATCH (to:TO) SET to=x RETURN to"))
                        .hasMessageContaining("Expected   cypher.null to be Element");
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void copyPropertiesToNull() {
        submitAndGet("CREATE (:FROM {prop1: 'a', prop2: 'b'})");

        List<Map<String, Object>> result = submitAndGet("OPTIONAL MATCH (x:NOT_EXISTING) WITH x MATCH (n:FROM) SET x=n RETURN x");

        assertThat(result)
            .extracting("x")
            .contains((Object) null);
    }

}
