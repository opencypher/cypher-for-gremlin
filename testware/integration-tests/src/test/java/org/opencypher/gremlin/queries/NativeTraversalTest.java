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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.extractor.Extractors.byName;
import static org.assertj.core.groups.FieldsOrPropertiesExtractor.extract;
import static org.opencypher.gremlin.test.TestCommons.ignoreOrderInCollections;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class NativeTraversalTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Before
    public void setUp() throws Exception {
        TestCommons.snGraph(gremlinServer.cypherGremlinClient());
    }

    @Test
    public void aggregateNothing() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");
        List<Map<String, Object>> count = submitAndGet("MATCH (n) RETURN count(n)");

        assertThat(count)
            .extracting("count(n)")
            .containsExactly(0L);
    }

    @Test
    public void pivotAndAggregateNothing() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");
        List<Map<String, Object>> count = submitAndGet("MATCH (n) RETURN count(n), keys(n)");
        assertThat(count).isEmpty();
    }

    @Test
    public void returnLiteral() throws Exception {
        List<Map<String, Object>> result = submitAndGet("RETURN 1 AS literal");
        assertThat(result)
            .extracting("literal")
            .containsExactly(1L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void returnFunction() throws Exception {
        List<Map<String, Object>> result = submitAndGet("RETURN toString(1) AS function");
        assertThat(result)
            .extracting("function")
            .containsExactly("1");
    }

    @Test
    public void limit() throws Exception {
        List<Map<String, Object>> result = submitAndGet("MATCH (n) RETURN n LIMIT 2");
        assertThat(result)
            .hasSize(2);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void aggregationFromPivot() throws Exception {
        List<Map<String, Object>> result = submitAndGet("match (n:Person) RETURN max(size(n.firstName)) as longest_name");
        assertThat(result)
            .extracting("longest_name")
            .containsExactly(6L);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void functionChain() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1.235] AS list\n" +
                "RETURN toString(toFloat(toInteger(list[0]))) AS chain"
        );
        assertThat(results)
            .extracting("chain")
            .containsExactly("1.0");
    }

    @Test
    public void mapLiteral() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {name: 'Matz', name2: 'Pontus'} AS map\n" +
                "RETURN exists(map.name) AS result"
        );
        assertThat(results)
            .extracting("result")
            .containsExactly(true);
    }

    @Test
    public void isNull() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (p:Person) RETURN p.notName IS NULL AS isNull, count(*)"
        );

        assertThat(results)
            .extracting("isNull", "count(*)")
            .containsExactly(tuple(true, 7L));
    }

    @Test
    public void nullOnMin() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");

        submitAndGet("CREATE (:kid), (:kid)");

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (k:kid) RETURN min(k.income) as income"
        );

        assertThat(results)
            .extracting("income")
            .contains((Object) null);
    }

    @Test
    public void oneAggregation() throws Exception {
        String cypher = "MATCH (n1) RETURN collect(n1)";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .hasSize(1)
            .flatExtracting("collect(n1)")
            .hasSize(18);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onePivotOneAggregation() throws Exception {
        String[] columnNames = {"n.lastName", "c1"};

        String cypher = "MATCH (n:Person) RETURN\n" +
            "n.lastName," +
            "collect(n.firstName) AS c1;";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting(Map::keySet)
            .allSatisfy(columns -> assertThat(columns).containsExactly(columnNames));

        assertThat(cypherResults)
            .hasSize(4)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("Andresen", asList("Erik", "Erik", "Erik", "Lars")),
                tuple("Lehtinen", asList("Olavi")),
                tuple("Haugland", asList("Martin")),
                tuple("Pedersen", asList("Erlend"))
            );
    }


    @Test
    @SuppressWarnings("unchecked")
    public void twoPivotsTwoAggregations() throws Exception {
        String[] columnNames = {"p1", "p2", "c1", "c2"};

        String cypher = "MATCH (n:Person) RETURN\n" +
            "n.lastName as p1," +
            "n.firstName as p2," +
            "count(n) AS c1," +
            "collect(n.email) AS c2;";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting(Map::keySet)
            .allSatisfy(columns -> assertThat(columns).containsExactly(columnNames));

        assertThat(cypherResults)
            .hasSize(5)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("Andresen", "Erik", 3L, asList("ErikJunior@gmx.com", "TheErik@gmail.com", "ErikTheSecond4@gmail.com")),
                tuple("Andresen", "Lars", 1L, asList("Lars.Nielsen.6711515243877401003@yahoo.com")),
                tuple("Lehtinen", "Olavi", 1L, asList("Olavi4398046541996@gmx.com")),
                tuple("Haugland", "Martin", 1L, asList("Martin4398046518130@hotmail.com")),
                tuple("Pedersen", "Erlend", 1L, asList("Erlend2199023287971@gmail.com"))
            );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onePivotThreeAggregations() throws Exception {
        String[] columnNames = {"n.lastName", "c1", "c2", "c3"};

        String cypher = "MATCH (n:Person) RETURN\n" +
            "n.lastName," +
            "count(n) AS c1," +
            "collect(n.born) AS c2," +
            "max(n.born) AS c3;";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting(Map::keySet)
            .allSatisfy(columns -> assertThat(columns).containsExactly(columnNames));

        assertThat(cypherResults)
            .hasSize(4)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("Andresen", 4L, asList(1995L, 1988L, 1988L, 1986L), 1995L),
                tuple("Lehtinen", 1L, asList(1983L), 1983L),
                tuple("Haugland", 1L, asList(1984L), 1984L),
                tuple("Pedersen", 1L, asList(1996L), 1996L)
            );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void threePivotsThreeAggregations() throws Exception {
        String[] columnNames = {"p1", "p2", "p3", "c1", "c2", "c3"};

        String cypher = "MATCH (n:Person) RETURN\n" +
            "n.lastName as p1," +
            "n.firstName as p2," +
            "n.born as p3," +
            "count(n) AS c1," +
            "collect(n.email) AS c2," +
            "max(n.born) AS c3;";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting(Map::keySet)
            .allSatisfy(columns -> assertThat(columns).containsExactly(columnNames));

        assertThat(cypherResults)
            .hasSize(6)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("Andresen", "Erik", 1988L, 2L, asList("TheErik@gmail.com", "ErikTheSecond4@gmail.com"), 1988L),
                tuple("Andresen", "Erik", 1995L, 1L, asList("ErikJunior@gmx.com"), 1995L),
                tuple("Andresen", "Lars", 1986L, 1L, asList("Lars.Nielsen.6711515243877401003@yahoo.com"), 1986L),
                tuple("Lehtinen", "Olavi", 1983L, 1L, asList("Olavi4398046541996@gmx.com"), 1983L),
                tuple("Haugland", "Martin", 1984L, 1L, asList("Martin4398046518130@hotmail.com"), 1984L),
                tuple("Pedersen", "Erlend", 1996L, 1L, asList("Erlend2199023287971@gmail.com"), 1996L)
            );
    }

    @Test
    public void pivotsAndAggregationsColumnOrdering() {
        String[] columnNames = {"c1", "p1", "c2", "p2"};

        String cypher = "MATCH (n:Person) RETURN\n" +
            "count(n) AS c1," +
            "n.lastName as p1," +
            "collect(n.email) AS c2," +
            "n.firstName as p2;";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting(Map::keySet)
            .allSatisfy(columns -> assertThat(columns).containsExactly(columnNames));
    }

    @Test
    public void distinct() throws Exception {
        String[] columnNames = {"n.lastName", "type(r)"};

        String cypher = "MATCH ()-[r]->(n:Person) RETURN DISTINCT n.lastName, type(r)";
        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .hasSize(3)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("Andresen", "KNOWS"),
                tuple("Pedersen", "KNOWS"),
                tuple("Lehtinen", "KNOWS")
            );
    }

    @Test
    public void aggregationsOnDifferentNodes() throws Exception {
        String[] columnNames = {"n.lastName", "type(r)", "collect(m.name)"};

        String cypher = "MATCH (n:Person)-[r]->(m:Interest) RETURN n.lastName, type(r), collect(m.name)";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .hasSize(2)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("Andresen", "LIKES", asList("Video", "Video", "Music")),
                tuple("Haugland", "LIKES", asList("Video"))
            );
    }

    @Test
    public void multiple() throws Exception {
        String[] columnNames = {"type(r)", "count"};

        String cypher = "MATCH ()-[r]->() RETURN count(r) AS count, type(r)";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .hasSize(4)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple("IS_LOCATED_IN", 6L),
                tuple("LIKES", 4L),
                tuple("KNOWS", 6L),
                tuple("IS_PART_OF", 6L)
            );
    }


    @Test
    public void aggregationsOnly() throws Exception {
        String[] columnNames = {"count(n1)", "count(n2)"};
        String cypher = "MATCH (n1)-[r]->(n2) RETURN count(n1), count(n2)";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .hasSize(1)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple(22L, 22L)
            );
    }

    @Test
    public void labelPredicate() throws Exception {
        String[] columnNames = {"isPerson", "count"};
        String cypher = "MATCH (n)\n" +
            "RETURN (n:Person) AS isPerson, count(*) AS count";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .hasSize(2)
            .extracting(columnNames)
            .usingElementComparator(ignoreOrderInCollections())
            .containsAll(extract(cypherResults, byName(columnNames)))
            .containsExactlyInAnyOrder(
                tuple(true, 7L),
                tuple(false, 11L)
            );
    }

    Comparator<? super Tuple> comparator = ignoreOrderInCollections();

    @Test
    public void testIgnoreOrderInCollections1() throws Exception {
        assertThat(comparator.compare(
            tuple("Andresen", asList("Erik", "Erik", "Erik", "Lars")),
            tuple("Andresen", asList("Erik", "Erik", "Erik", "Lars"))
        )).isEqualTo(0);
    }

    @Test
    public void testIgnoreOrderInCollections2() throws Exception {
        assertThat(comparator.compare(
            tuple("Andresen", asList("Lars", "Erik", "Erik", "Erik")),
            tuple("Andresen", asList("Erik", "Erik", "Erik", "Lars"))
        )).isEqualTo(0);
    }

    @Test
    public void testIgnoreOrderInCollections3() throws Exception {
        assertThat(comparator.compare(
            tuple("Andresen", asList("Lars", "Erik", "Erik", "Erik")),
            tuple("Andresen", asList("Erik", "Erik", "Lars"))
        )).isNotEqualTo(0);
    }

    @Test
    public void testIgnoreOrderInCollections4() throws Exception {
        assertThat(comparator.compare(
            tuple("Andresen", asList(null, "Erik", "Erik", "Erik")),
            tuple("Andresen", asList("Erik", "Erik", "Lars", null))
        )).isNotEqualTo(0);
    }

    @Test
    public void testIgnoreOrderInCollections5() throws Exception {
        assertThat(comparator.compare(
            tuple("Andresen", asList(null, "Erik", "Erik", "Lars")),
            tuple("Andresen", asList(null, "Erik", "Erik", "Lars"))
        )).isEqualTo(0);
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void math() throws Exception {
        String cypher = "MATCH (n:Person {firstName: 'Erlend'}) " +
            "RETURN toInteger(sqrt(abs(1 + (2 - (3 * (4 / (5 ^ ((n.born - 1990) % 3)))))))) AS result";

        List<Map<String, Object>> cypherResults = submitAndGet(cypher);

        assertThat(cypherResults)
            .extracting("result")
            .containsExactly(3L);
    }

    @Test
    public void failOnInvalidAggregation() throws Exception {
        assertThatThrownBy(() -> submitAndGet("RETURN count(rand())"))
                .hasMessageContaining("Can't use non-deterministic (random) functions inside of aggregate functions");

        assertThatThrownBy(() -> submitAndGet("RETURN count(toInteger(rand()+1))"))
                .hasMessageContaining("Can't use non-deterministic (random) functions inside of aggregate functions");

        assertThatThrownBy(() -> submitAndGet("RETURN count(count(*))"))
                .hasMessageContaining("Can't use aggregate functions inside of aggregate functions");

        assertThatThrownBy(() -> submitAndGet("MATCH (n) RETURN count(toInteger(avg(n.prop)) + 1)"))
                .hasMessageMatching(".*(contains child expressions which are aggregations|" +
                    "Can't use aggregate functions inside of aggregate function).*");
    }
}
