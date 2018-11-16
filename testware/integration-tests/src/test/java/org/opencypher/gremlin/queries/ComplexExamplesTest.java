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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;
import static org.opencypher.gremlin.translation.ReturnProperties.LABEL;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ComplexExamplesTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Before
    public void setUp() {
        submitAndGet(DELETE_ALL);
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void returnDuplicateNode() throws Exception {
        submitAndGet(
            "CREATE (x:root)-[:r]->(y)\n" +
                "CREATE (x)-[:r]->(z)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (x:root)-[:r]->()\n" +
                "RETURN x"
        );
        List<Map<String, Object>> distinctResults = submitAndGet(
            "MATCH (x:root)-[:r]->()\n" +
                "RETURN DISTINCT x"
        );

        assertThat(results)
            .hasSize(2)
            .matches(rows -> {
                Object first = rows.get(0);
                Object second = rows.get(1);
                return Objects.equals(first, second);
            });

        assertThat(distinctResults)
            .hasSize(1)
            .extracting("x")
            .extracting(LABEL)
            .containsExactly("root");
    }

    @Test
    public void filterOutBasedOnNodePropName() throws Exception {
        submitAndGet("CREATE ({name: 'Someone'})<-[:X]-()-[:X]->({name: 'Andres'})");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH ()-[rel:X]-(a)\n" +
                "WHERE a.name = 'Andres'\n" +
                "RETURN a"
        );

        assertThat(results)
            .hasSize(1)
            .extracting("a")
            .extracting(byElementProperty("name"))
            .containsExactly("Andres");
    }

    @Test
    public void returnTwoSubgraphsWithBoundUndirectedRelationship() throws Exception {
        submitAndGet("CREATE (a:A {value: 1})-[:REL {name: 'r'}]->(b:B {value: 2})");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)-[r {name: 'r'}]-(b)\n" +
                "RETURN a.value, b.value"
        );

        assertThat(results)
            .hasSize(2)
            .extracting("a.value", "b.value")
            .containsExactlyInAnyOrder(
                tuple(1L, 2L),
                tuple(2L, 1L)
            );
    }

    @Test
    public void returnChildrenPairsWithoutSelfPairs() throws Exception {
        submitAndGet(
            "CREATE (ss:StarSystem {name: 'Sirius'})\n" +
                "CREATE (s1:Star {name: 'Sirius A'})-[:MEMBER_OF]->(ss)\n" +
                "CREATE (s2:Star {name: 'Sirius B'})-[:MEMBER_OF]->(ss)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (s1:Star)-->(:StarSystem {name: 'Sirius'})<--(s2:Star)\n" +
                "RETURN s1.name, s2.name"
        );

        assertThat(results)
            .hasSize(2)
            .extracting("s1.name", "s2.name")
            .containsExactlyInAnyOrder(
                tuple("Sirius A", "Sirius B"),
                tuple("Sirius B", "Sirius A")
            );
    }

    @Test
    public void matchInvalidProp() throws Exception {
        submitAndGet(
            "CREATE (a {name: 'A'}), (b {name: 'B'}), (c {name: 'C'})\n" +
                "CREATE (a)-[:KNOWS]->(b)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a {name: 'A'}), (c {name: 'C'})\n" +
                "MATCH (a)-->(b)\n" +
                "RETURN a.name, b.name, c.name"
        );

        assertThat(results)
            .hasSize(1)
            .extracting("a.name", "b.name", "c.name")
            .containsExactly(tuple("A", "B", "C"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void returnExpressionUnrelatedToMatch() throws Exception {
        submitAndGet("CREATE ()-[:T]->()");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)-[r:T]->()\n" +
                "RETURN toString(1) AS x"
        );

        assertThat(results)
            .extracting("x")
            .containsExactly("1");
    }

    @Test
    public void selfReferentialNode() throws Exception {
        submitAndGet("CREATE (n:N {n: 1})-[:R]->(n)");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)-[r]->(a) RETURN a.n"
        );

        assertThat(results)
            .extracting("a.n")
            .containsExactly(1L);
    }

    @Test
    public void selfReferentialNodeChained() throws Exception {
        submitAndGet("CREATE (a:A {n: 1})-[:R]->(b:B {n: 2})-[:R]->(b)");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)-[r1]->(b)-[r2]->(b) RETURN a.n, b.n"
        );

        assertThat(results)
            .extracting("a.n", "b.n")
            .containsExactly(tuple(1L, 2L));
    }

    @Test
    public void selfReferentialNodeInTwoPatterns() throws Exception {
        submitAndGet("CREATE (a:A {n: 1})-[:R]->(b:B {n: 2})-[:R]->(b)");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)-[r1]->(b), (b)-[r2]->(b) RETURN a.n, b.n"
        );

        assertThat(results)
            .extracting("a.n", "b.n")
            .containsExactly(tuple(1L, 2L));
    }

    @Test
    public void selfReferentialNodeUndirected() throws Exception {
        submitAndGet(
            "CREATE " +
                "(a:A {name: 'a'})-[:R]->(l:Looper {name: 'l'}), " +
                "(l)-[:LOOP]->(l)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)--(b) " +
                "RETURN a.name, b.name"
        );

        assertThat(results)
            .extracting("a.name", "b.name")
            .containsExactlyInAnyOrder(
                tuple("a", "l"),
                tuple("l", "a"),
                tuple("l", "l")
            );
    }

    @Test
    public void selfReferentialNodeWithUndirectedSegment() throws Exception {
        submitAndGet(
            "CREATE (:A {name: 'a'})-[:T1]->(l:Looper {name: 'l'}), " +
                "(l)-[:LOOP]->(l), " +
                "(l)-[:T2]->(:B {name: 'b'})"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)--(b)--(c) " +
                "RETURN a.name, b.name, c.name"
        );

        assertThat(results)
            .extracting("a.name", "b.name", "c.name")
            .containsExactlyInAnyOrder(
                tuple("a", "l", "l"),
                tuple("a", "l", "b"),
                tuple("l", "l", "a"),
                tuple("l", "l", "b"),
                tuple("b", "l", "l"),
                tuple("b", "l", "a")
            );
    }

    @Test
    public void biDirectionalPath() throws Exception {
        submitAndGet(
            "CREATE " +
                "(a:A {name: 'a'}), (b:B {name: 'b'}), " +
                "(a)-[:R1 {name: 'r1'}]->(b), " +
                "(b)-[:R2 {name: 'r2'}]->(a)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a)-[r1]-(b)-[r2]-(c) " +
                "RETURN a.name, r1.name, b.name, r2.name, c.name"
        );

        assertThat(results)
            .extracting("a.name", "r1.name", "b.name", "r2.name", "c.name")
            .containsExactlyInAnyOrder(
                tuple("a", "r1", "b", "r2", "a"),
                tuple("a", "r2", "b", "r1", "a"),
                tuple("b", "r1", "a", "r2", "b"),
                tuple("b", "r2", "a", "r1", "b")
            );
    }

    @Test
    public void matchAndReverseOptionalMatch() throws Exception {
        submitAndGet("CREATE (:A {name: 'A'})-[:T {name: 'T'}]->(:B {name: 'B'})");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (a1)-[r]->() " +
                "WITH r, a1 " +
                "OPTIONAL MATCH (a1)<-[r]-(b2) " +
                "RETURN a1.name, r.name, b2.name"
        );

        assertThat(results)
            .extracting("a1.name", "r.name", "b2.name")
            .containsExactly(tuple("A", "T", null));
    }

    @Test
    public void matchRhsAliases() throws Exception {
        submitAndGet("CREATE (h1:House {name: 'house1', animal:'cat'}), " +
            "(h2:House {name: 'house2', animal:'dog'}), " +
            "(h3:House {name: 'house3', animal:'cat'}), " +
            "(h4:House {name: 'house4', animal:'cat'}), " +
            "(h1)-[:KNOWS]->(h2), " +
            "(h1)-[:KNOWS]->(h3), " +
            "(h1)-[:KNOWS]->(h4)");

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)-[rel]->(x)\n" +
                "WHERE n.animal = x.animal\n" +
                "RETURN n.name, x.name"
        );

        assertThat(results)
            .extracting("n.name", "x.name")
            .containsExactlyInAnyOrder(tuple("house1", "house3"),
                tuple("house1", "house4"));
    }

    @Test
    public void stringInequality() {
        submitAndGet(
            "CREATE (root:Root {name: 'x'}), " +
                "(child1:TextNode {prop2: 'text'}), " +
                "(child2:IntNode {prop2: 0}), " +
                "(root)-[:T]->(child1), " +
                "(root)-[:T]->(child2)"
        );
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (:Root {name: 'x'})-->(i:TextNode) " +
                "WHERE i.prop2 > 'te' " +
                "RETURN i.prop2 as prop"
        );

        assertThat(results)
            .extracting("prop")
            .containsExactly("text");
    }

    @Test
    public void optionalMatchOnEmptyGraph() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH (n) " +
                "RETURN n"
        );

        assertThat(results)
            .extracting("n")
            .containsExactly((Object) null);
    }

    @Test
    public void doubleWithMerge() throws Exception {
        submitAndGet("CREATE ({id: 0})");
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "WITH n AS a, n AS b " +
                "WITH a AS x " +
                "MERGE (a) " +
                "RETURN x.id AS x"
        );

        assertThat(results)
            .extracting("x")
            .containsExactly(0L);
    }
}
