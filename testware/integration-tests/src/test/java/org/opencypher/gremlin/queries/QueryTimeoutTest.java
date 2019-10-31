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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.client.CypherGremlinClient;
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

    private static final String SLOW_QUERY = "UNWIND range(0,10000) as a CREATE (n :test {test: a}) RETURN 'ok' AS ok";

    @Test
    public void gremlinTimeout() throws Exception {
        Client client = gremlinServer.gremlinClient();

        String gremlin = new TranslationFacade().toGremlinGroovy(SLOW_QUERY);

        assertThatThrownBy(() -> client.submit(gremlin).all().get())
            .hasMessageMatching("(?i).*timeout.*");
    }

    @Test
    public void pluginTimeout() throws Exception {
        CypherGremlinClient client = gremlinServer.cypherGremlinClient();

        assertThatThrownBy(() -> client.submit(SLOW_QUERY).all())
            .hasMessageMatching("(?i).*timeout.*");
    }

    @Test
    public void pluginConfigurableTimeout() throws Exception {
        CypherGremlinClient client = gremlinServer.cypherGremlinClient();

        List<Map<String, Object>> result = client.statement(SLOW_QUERY)
            .withTimeout(10, SECONDS)
            .submit().get().all();

        assertThat(result)
            .hasSize(10001)
            .allMatch(e -> e.get("ok").equals("ok"));
    }
}
