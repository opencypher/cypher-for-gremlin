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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.server.EmbeddedGremlinServer;
import org.opencypher.gremlin.translation.TranslationFacade;

public class QueryTimeoutTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(
        (c) -> {
        },
        () -> EmbeddedGremlinServer.builder()
            .defaultParameters()
            .scriptEvaluationTimeout(3)
            .build()
    );

    private static final String SLOW_QUERY = "UNWIND range(0,10000) as a CREATE (n :test {test: a})";

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void testPluginGremlin() throws Exception {
        Client client = gremlinServer.gremlinClient();

        String gremlin = new TranslationFacade().toGremlinGroovy(SLOW_QUERY);

        assertThatThrownBy(() -> client.submit(gremlin).all().get())
            .hasMessageContaining("scriptEvaluationTimeout");
    }

    @Test
    public void testPluginTimeout() throws Exception {
        assertThatThrownBy(() -> submitAndGet(SLOW_QUERY))
            .hasMessageContaining("scriptEvaluationTimeout");
    }
}
