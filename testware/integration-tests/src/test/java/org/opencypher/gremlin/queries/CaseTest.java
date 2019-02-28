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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithCosmosDB;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class CaseTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    @Before
    public void setUp() throws Exception {
        TestCommons.modernGraph(gremlinServer.cypherGremlinClient());
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleFormNumericMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) RETURN " +
                "CASE n.age" +
                "  WHEN 35 THEN 'tetrahedral' " +
                "  WHEN 27 THEN 'smith' " +
                "  WHEN 29 THEN 'markov' " +
                "  ELSE 'boring' " +
                "END AS number"
        );

        assertThat(results)
            .extracting("number")
            .containsExactlyInAnyOrder("tetrahedral", "smith", "markov", "boring");
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleFormStringMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:software) RETURN\n" +
                "CASE n.name " +
                "  WHEN 'lop' THEN 1 " +
                "  WHEN 'ripple' THEN 2 " +
                "END AS index"
        );

        assertThat(results)
            .extracting("index")
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleFormPartialMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) RETURN " +
                "CASE n.age " +
                "  WHEN 35 THEN 'tetrahedral' " +
                "END AS number"
        );

        assertThat(results)
            .extracting("number")
            .containsExactlyInAnyOrder("tetrahedral", null, null, null);
    }

    @Test
    @Category(SkipWithCosmosDB.Choose.class)
    public void predicateMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN " +
                "CASE " +
                "  WHEN n.name = 'marko' THEN 1 " +
                "  WHEN n.age < 40 THEN 2 " +
                "  ELSE 3 " +
                "END AS result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(1L, 2L, 3L, 2L, 3L, 2L);
    }

    @Test
    @Category(SkipWithCosmosDB.Choose.class)
    public void orderWhenMatching2Predicates() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) RETURN\n" +
                "CASE " +
                "    WHEN n.name = 'marko' THEN 'one'" +
                "    WHEN n.age < 30 THEN 0 " +
                "END AS result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder("one", 0L, null, null);
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleFormMatchUnexpectedNulls() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN " +
                "CASE n.age" +
                "  WHEN 32 THEN 'bingo'" +
                "END as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder("bingo", null, null, null, null, null);
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleMatchDifferentTypes() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [13, 3.14, 'bingo', true, null, ['a']] AS n RETURN CASE n " +
                "  WHEN 13 THEN 'integer' " +
                "  WHEN 3.14 THEN 'float' " +
                "  WHEN 'bingo' THEN 'string' " +
                "  WHEN true THEN 'boolean' " +
                "  WHEN null THEN 'null' " +
                "  WHEN ['a'] THEN 'collection' " +
                "END as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder("integer", "float", "string", "boolean", "null", "collection");
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleReturnDifferentTypes() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN " +
                "CASE n.name" +
                "  WHEN 'marko' THEN 13 " +
                "  WHEN 'vadas' THEN 3.14 " +
                "  WHEN 'josh' THEN 'bingo' " +
                "  WHEN 'peter' THEN true " +
                "  WHEN 'lop' THEN null " +
                "  WHEN 'ripple' THEN ['a'] " +
                "END as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(13L, 3.14, "bingo", true, null, asList("a"));
    }

    @Test
    @Category(SkipWithCosmosDB.Choose.class)
    public void returnDifferentTypes() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) RETURN " +
                "CASE" +
                "  WHEN n.age = 29 THEN 13 " +
                "  WHEN n.age = 27 THEN 3.14 " +
                "  WHEN n.age = 32 THEN 'bingo' " +
                "  WHEN n.age = 35 THEN true " +
                "  WHEN n.name = 'lop' THEN null " +
                "  WHEN n.name = 'ripple' THEN ['a'] " +
                "END as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(13L, 3.14, "bingo", true, null, asList("a"));
    }

    @Test
    @Category(SkipWithCosmosDB.NoMath.class)
    public void labelPredicatesWithAggregationProjection() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) " +
                "WITH CASE " +
                "  WHEN n:person THEN true " +
                "  WHEN n:software THEN false " +
                "END AS result, count(n) as count " +
                "RETURN result, count * 2 as test"
        );

        assertThat(results)
            .extracting("result", "test")
            .containsExactlyInAnyOrder(tuple(true, 8L), tuple(false, 4L));
    }

    @Test
    @Category(SkipWithCosmosDB.Choose.class)
    public void caseInWhere() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) WHERE " +
                "(CASE " +
                "  WHEN n.name= 'josh' THEN 1 " +
                "  ELSE -1 " +
                "END) > 0 " +
                "RETURN n.age as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(32L);
    }

    @Test
    @Category(SkipWithCosmosDB.NoNoneToken.class)
    public void simpleCaseInWhere() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n) WHERE " +
                "(CASE n.name " +
                "  WHEN 'josh' THEN 1 " +
                "  ELSE -1 " +
                "END) > 0 " +
                "RETURN n.age as result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(32L);
    }

    @Test
    public void expressionsInSimpleCase() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person) RETURN \n" +
                "CASE split(n.name, 'a')[0]\n" +
                "   WHEN 'm' THEN 1\n" +
                "   WHEN 'v' THEN 2\n" +
                "   ELSE NULL\n" +
                "END as r"

        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder(1L, 2L, null, null);
    }

    @Test
    public void expressionsInSimpleCaseForm() throws Exception {
        submitAndGet("CREATE (:test {name: 'a_a'}), (:test {name: 'a_b'}), (:test {name: 'a_c'})");

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:test)\n" +
                "RETURN \n" +
                "  CASE split(n.name, '_')[0] \n" +
                "    WHEN split(n.name, '_')[1] THEN n.name\n" +
                "    ELSE NULL\n" +
                "  END as r\n"
        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("a_a", null, null);
    }

    @Test
    public void expressionsInSimpleCaseFormInWhere() throws Exception {
        submitAndGet("CREATE (:test {name: 'a_a'}), (:test {name: 'a_b'}), (:test {name: 'a_c'})");

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)\n" +
                "WHERE \n" +
                "  CASE split(n.name, '_')[0] \n" +
                "    WHEN split(n.name, '_')[1] THEN n.name\n" +
                "    ELSE NULL\n" +
                "  END\n" +
                "IS NOT NULL\n" +
                "RETURN n.name as r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("a_a");
    }
}
