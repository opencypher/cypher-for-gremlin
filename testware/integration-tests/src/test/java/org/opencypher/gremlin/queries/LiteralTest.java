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
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class LiteralTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void literals() {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN [" +
                "13, -40000," +
                "'Hello', \"World\"," +
                "true, false, TRUE, FALSE," +
                "null," +
                "[], [13, 'Hello', true, null]" +
                "] AS literals"
        );

        assertThat(results)
            .extracting("literals")
            .containsExactly(asList(
                13L, -40000L,
                "Hello", "World",
                true, false, true, false,
                null,
                emptyList(), asList(13L, "Hello", true, null)
            ));
    }

}
