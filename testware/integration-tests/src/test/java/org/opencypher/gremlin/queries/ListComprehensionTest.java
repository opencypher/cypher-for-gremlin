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
import static org.opencypher.gremlin.test.TestCommons.ignoreOrderInCollections;
import static org.opencypher.gremlin.translation.ReturnProperties.LABEL;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ListComprehensionTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Before
    public void setUp() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n;");
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void listComprehensionInFirstReturnStatement() throws Exception {
        String cypher = "RETURN [x IN [1, 2.3, true, 'apa'] | toString(x) ] AS list";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results).hasSize(1);
        assertThat(results)
            .extracting("list")
            .containsExactly(asList("1", "2.3", "true", "apa"));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void simplestCaseOfListComprehension() throws Exception {
        String cypher = "WITH [2, 2.9] AS numbers\n" +
            " RETURN [n IN numbers | toInteger(n)] AS int_numbers";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .extracting("int_numbers")
            .containsExactly(asList(2L, 2L));
    }

    @Test
    @Category(UsesExtensions.CustomFunctions.class)
    public void applyMultipleFunctions() throws Exception {
        String cypher = "WITH [2, 2.9] AS numbers\n" +
            " RETURN [n IN numbers | toString(toInteger(n))] AS int_numbers";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .extracting("int_numbers")
            .containsExactly(asList("2", "2"));
    }

    @Test
    public void patternComprehension() throws Exception {
        submitAndGet("CREATE (a:Person { name: 'Charlie Sheen' })\n" +
            "CREATE (m1:Movie { name: 'Wall Street', year: 1987 })\n" +
            "CREATE (m2:Movie { name: 'Alpaca Now', year: 1979 })\n" +
            "CREATE (m3:Movie { name: 'Red Down', year: 1984 })\n" +
            "CREATE (m4:Show { name: 'Two and a Half Men', year: 2003 })\n" +
            "CREATE (a)-[:ACTED_IN]->(m1)\n" +
            "CREATE (a)-[:ACTED_IN]->(m2)\n" +
            "CREATE (a)-[:ACTED_IN]->(m3)\n" +
            "CREATE (a)-[:ACTED_IN]->(m4)");

        String cypher = "MATCH (a:Person { name: 'Charlie Sheen' })\n" +
            "RETURN [(a)-->(b) WHERE b:Movie | b.year] AS years";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .extracting("years")
            .usingElementComparator(ignoreOrderInCollections())
            .containsExactly(asList(1987L, 1979L, 1984L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void pathInPatternComprehension() throws Exception {
        submitAndGet("CREATE (a:A), (:A), (:A) " +
            "      CREATE (a)-[:HAS]->(:B)");

        String cypher = "MATCH (n:A) " +
            "RETURN [p = (n)-[:HAS]->() | p] AS ps";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(3)
            .extracting("ps")
            .flatExtracting(m -> ((Collection) m))
            .flatExtracting(m -> m               )
            .extracting("_label")
            .contains("A", "HAS", "B");
    }

    @Test
    public void varLengthPathInPatternComprehension() throws Exception {
        submitAndGet("CREATE (:A)-[:T]->(:B)");

        String cypher = "MATCH (a:A), (b:B) " +
            "WITH [p = (a)-[*]->(b) | p] AS paths, count(a) AS c " +
            "RETURN paths, c";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("c")
            .containsExactly(1L);
        assertThat(results)
            .extracting("paths")
            .hasSize(1)
            .allSatisfy(paths -> {
                assertThat(paths)
                    .asList()
                    .hasSize(1);
                assertThat(((List) paths).get(0))
                    .asList()
                    .extracting(LABEL)
                    .containsExactly("A", "T", "B");
            });
    }

    @Test
    public void patternComprehensionNodeDegree() throws Exception {
        submitAndGet("CREATE (x:X),\n" +
            "(x)-[:T]->(),\n" +
            "(x)-[:T]->(),\n" +
            "(x)-[:T]->()");

        String cypher = "MATCH (a:X)\n" +
            "RETURN size([(a)-->() | 1]) AS length";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("length")
            .containsExactly(3L);
    }
}
