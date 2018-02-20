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
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithGremlinGroovy;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ContainerIndexTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void listIndexInReturn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN list[1] AS i"
        );
        assertThat(results)
            .extracting("i")
            .containsExactly(2L);
    }

    @Test
    public void listIndexInReturnFunction() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN toString(list[1]) AS s"
        );
        assertThat(results)
            .extracting("s")
            .containsExactly("2");
    }

    /**
     * Maps don't work in Gremlin Groovy translation
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void mapIndexInReturn() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: 1, bar: 2, baz: 3} AS map\n" +
                "RETURN map['bar'] AS i"
        );
        assertThat(results)
            .extracting("i")
            .containsExactly(2L);
    }

    /**
     * Maps don't work in Gremlin Groovy translation
     */
    @Test
    @Category(SkipWithGremlinGroovy.class)
    public void mapIndexInReturnFunction() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH {foo: 1, bar: 2, baz: 3} AS map\n" +
                "RETURN toString(map['bar']) AS s"
        );
        assertThat(results)
            .extracting("s")
            .containsExactly("2");
    }
}
