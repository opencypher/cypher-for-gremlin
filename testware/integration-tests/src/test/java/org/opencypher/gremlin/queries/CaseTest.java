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

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class CaseTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void numericMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person)\n" +
                "RETURN\n" +
                "  CASE n.age\n" +
                "  WHEN 35\n" +
                "  THEN 'tetrahedral'\n" +
                "  WHEN 27\n" +
                "  THEN 'smith'\n" +
                "  WHEN 29\n" +
                "  THEN 'markov'\n" +
                "  ELSE 'boring'\n" +
                "END AS number"
        );

        assertThat(results)
            .extracting("number")
            .containsExactlyInAnyOrder("tetrahedral", "smith", "markov", "boring");
    }

    @Test
    public void stringMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:software) RETURN\n" +
                "CASE n.name\n" +
                "  WHEN 'lop'\n" +
                "  THEN 1\n" +
                "  WHEN 'ripple'\n" +
                "  THEN 2\n" +
                "END\n" +
                "AS index"
        );

        assertThat(results)
            .extracting("index")
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void partialMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n:person)\n" +
                "RETURN\n" +
                "  CASE n.age\n" +
                "  WHEN 35\n" +
                "  THEN 'tetrahedral'\n" +
                "END AS number"
        );

        assertThat(results)
            .extracting("number")
            .containsExactlyInAnyOrder("tetrahedral", null, null, null);
    }

    @Test
    public void predicateMatch() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (n)\n" +
                "RETURN\n" +
                "CASE\n" +
                "WHEN n.name = 'marko'\n" +
                "THEN 1\n" +
                "WHEN n.age < 40\n" +
                "THEN 2\n" +
                "ELSE 3 END AS result"
        );

        assertThat(results)
            .extracting("result")
            .containsExactlyInAnyOrder(1L, 2L, 3L, 2L, 3L, 2L);
    }

    //test predicate

    //test expression is empty
}
