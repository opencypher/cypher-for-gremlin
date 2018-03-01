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
package org.opencypher.gremlin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class BytecodeCypherGremlinClientTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private BytecodeCypherGremlinClient client;

    @Before
    public void setUp() {
        Client aliasedClient = gremlinServer.gremlinClient().alias("g");
        client = new BytecodeCypherGremlinClient(aliasedClient, TranslatorFlavor.gremlinServer());
    }

    @Test
    public void submitToDefaultGraph() {
        String cypher = "MATCH (p:person) RETURN p.name AS name";
        List<Map<String, Object>> results = client.submit(cypher).all();

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "vadas", "josh", "peter");
    }

    @Test
    public void invalidSyntax() {
        CypherResultSet resultSet = client.submit("INVALID");
        Throwable throwable = catchThrowable(resultSet::all);

        assertThat(throwable)
            .hasMessageContaining("Invalid input");
    }
}
