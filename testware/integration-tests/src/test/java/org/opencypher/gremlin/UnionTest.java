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

import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.TinkerGraphServerEmbedded;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class UnionTest {

    @ClassRule
    public static final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.client().submitCypher(cypher);
    }

    @Test
    public void simpleUnion() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN 'john' AS name " +
                "UNION " +
                "RETURN 'jane' AS name " +
                "UNION " +
                "RETURN 'john' AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactly("john", "jane");
    }

    @Test
    public void simpleUnionAll() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN 'john' AS name " +
                "UNION ALL " +
                "RETURN 'jane' AS name " +
                "UNION ALL " +
                "RETURN 'john' AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactly("john", "jane", "john");
    }

    @Test
    public void developers() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND ['stephen', 'daniel', 'marko'] AS name RETURN name " +
                "UNION ALL " +
                "MATCH (p:person)-[:created]->() RETURN DISTINCT p.name AS name"
        );

        assertThat(results)
            .extracting("name")
            .containsExactly("stephen", "daniel", "marko", "marko", "josh", "peter");
    }

    @Test
    public void languages() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND ['clojure'] AS lang RETURN lang " +
                "UNION " +
                "UNWIND ['java', 'scala'] AS lang RETURN lang " +
                "UNION " +
                "MATCH (s:software) RETURN s.lang AS lang"
        );

        assertThat(results)
            .extracting("lang")
            .containsExactly("clojure", "java", "scala");
    }

    @Test
    public void counts() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "MATCH (s:software) RETURN count(s) AS num " +
                "UNION ALL " +
                "MATCH (p:person) RETURN count(p) AS num"
        );

        assertThat(results)
            .extracting("num")
            .containsExactly(2L, 4L);
    }
}
