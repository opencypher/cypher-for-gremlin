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
package org.opencypher.gremlin;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithGremlinGroovy;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

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

    /**
     * List Comprehension don't work in Gremlin Groovy translation
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void listComprehensionInFirstReturnStatement() throws Exception {
        String cypher = "RETURN [x IN [1, 2.3, true, 'apa'] | toString(x) ] AS list";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results).hasSize(1);
        assertThat(results)
            .extracting("list")
            .containsExactly(asList("1", "2.3", "true", "apa"));
    }

    /**
     * List Comprehension don't work in Gremlin Groovy translation
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void simplestCaseOfListComprehension() throws Exception {
        String cypher = "WITH [2, 2.9] AS numbers\n" +
            " RETURN [n IN numbers | toInteger(n)] AS int_numbers";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .extracting("int_numbers")
            .containsExactly(asList(2L, 2L));
    }

    /**
     * List Comprehension don't work in Gremlin Groovy translation
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void applyMultipleFunctions() throws Exception {
        String cypher = "WITH [2, 2.9] AS numbers\n" +
            " RETURN [n IN numbers | toString(toInteger(n))] AS int_numbers";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .hasSize(1)
            .extracting("int_numbers")
            .containsExactly(asList("2", "2"));
    }

    /**
     * List Comprehension don't work in Gremlin Groovy translation
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
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
            .containsExactly(asList(1987L, 1979L, 1984L));
    }

    @Test
    public void pathInPatternComprehension() throws Exception {
        submitAndGet("CREATE (a:A), (b:B)\n" +
            "CREATE (a)-[:T]->(b)");

        String cypher = "MATCH (a:A), (b:B)\n" +
            "RETURN [p = (a)-[*]->(b) | p] AS paths";

        List<Map<String, Object>> results = submitAndGet(cypher);

        assertThat(results)
            .extracting("paths")
            .hasSize(1)
            .allSatisfy(paths -> {
                assertThat(paths)
                    .asList()
                    .hasSize(1);
                assertThat(((List) paths).get(0))
                    .asList()
                    .extracting("label")
                    .containsExactly("A", "T", "B");
            });
    }
}
