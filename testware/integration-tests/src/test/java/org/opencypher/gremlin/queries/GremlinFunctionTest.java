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
package org.opencypher.gremlin.queries;

import static org.apache.tinkerpop.gremlin.structure.util.ElementHelper.asMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.client.CypherResultSet;
import org.opencypher.gremlin.groups.SkipWithPlugin;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

/**
 * Currently experimental `gremlin` function is supported only for client-side translation, and should be enabled explicitly.
 * Run this test with `-Dtranslate=gremlin+cfog_server_extensions+experimental_gremlin_function` or
 * `-Dtranslate=bytecode+cfog_server_extensions+experimental_gremlin_function`
 */
@Category(SkipWithPlugin.class)
public class GremlinFunctionTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void testGremlinFunction() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person {name: 'marko'}) RETURN gremlin(\"select('n').outE().label()\") as r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("created");
    }

    @Test
    public void testGremlinFunctionInWith() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH gremlin(\"g.V().has('name', eq('marko'))\") AS n RETURN n.name as r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("marko");
    }

    @Test
    public void testGremlinFunctionMultipleValues() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH gremlin(\"constant(1).as('a').constant(2).as('b').select('a', 'b')\") AS constants " +
                " RETURN constants['a'] as a, constants['b'] as b"
        );

        assertThat(results)
            .extracting("a", "b")
            .containsExactlyInAnyOrder(tuple(1L, 2L));
    }

    @Test
    public void gremlinAsParameter() {
        CypherGremlinClient client = gremlinServer.cypherGremlinClient();

        String gremlin = "select('n').values('name').as('l')";
        String cypher = "MATCH (n:person)" +
            "RETURN gremlin({gremlinQuery}) AS l";

        CypherResultSet results = client.submit(cypher, asMap("gremlinQuery", gremlin));

        assertThat(results)
            .extracting("l")
            .containsExactlyInAnyOrder("josh", "vadas", "peter", "marko");
    }

    @Test
    public void dontOverwriteValues() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH 1 as a, gremlin(\"constant(2).as('a')\") as b " +
                "RETURN a, b"
        );

        assertThat(results)
            .extracting("a", "b")
            .containsExactlyInAnyOrder(tuple(1L, 2L));
    }

    @Test
    public void useInMatch() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH gremlin(\"g.V().has('name', eq('marko'))\") AS n " +
                "MATCH (n)-[:knows]->(m) " +
                "RETURN m.name as r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly("vadas", "josh");
    }


    @Test
    public void singleValue() {
        List<Map<String, Object>> results = submitAndGet(
            "WITH gremlin(\"g.V().hasLabel('person')\") AS node RETURN id(node) as r"
        );

        assertThat(results)
            .hasSize(1)
            .extracting("r")
            .containsExactly(0L);
    }

    @Test
    public void list() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND gremlin(\"g.V().hasLabel('person').fold()\") AS node RETURN node.name as r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly("marko", "vadas", "josh", "peter");
    }

    /**
     * Courtesy of <a href="https://stackoverflow.com/a/42097722">Daniel Kuppitz</a>
     */
    @Test
    public void kahnsAlgorithm() {
        List<Map<String, Object>> results = submitAndGet(

            "WITH gremlin(\"g.V().not(inE()).aggregate('x')." +
                "           repeat(outE().aggregate('e').inV().not(inE().where(without('e'))).aggregate('x'))." +
                "         cap('x')\") as x " +
                "UNWIND x AS n " +
                "RETURN n.name as name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "peter", "vadas", "josh", "ripple", "lop");
    }

    @Test
    public void lowestCommonAncestor() {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (v {name:'vadas'}), (l {name:'lop'})" +
                "WITH gremlin(\"" +
                "select('l').emit().repeat(inE().outV()).as('x')" +
                ".repeat(outE().inV()).emit(where(eq('v')))" +
                ".select('x')\") AS x RETURN x.name as r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("marko");
    }
}
