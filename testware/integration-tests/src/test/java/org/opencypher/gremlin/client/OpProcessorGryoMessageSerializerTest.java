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
package org.opencypher.gremlin.client;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerIoRegistryV3d0;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.server.EmbeddedGremlinServer;
import org.opencypher.gremlin.test.TestCommons;

public class OpProcessorGryoMessageSerializerTest {
    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(
        TestCommons::modernGraph,
        () -> EmbeddedGremlinServer.builder()
            .defaultParameters()
            .serializer(getGryoV3D0Serializer().getClass(), singletonList(TinkerIoRegistryV3d0.class))
            .build()
    );

    @Test
    public void gryoMessageSerializer() {
        Client gremlinClient = Cluster.build()
            .port(gremlinServer.getPort())
            .serializer(getGryoV3D0Serializer())
            .create()
            .connect();

        CypherGremlinClient client = CypherGremlinClient.plugin(gremlinClient);
        List<Map<String, Object>> results = client.submit("MATCH (n {name: 'marko'}) RETURN n").all();

        assertThat(results)
            .extracting("n")
            .extracting("_type", "_label", "name", "age")
            .containsExactly(tuple("node", "person", "marko", 29L));
    }

    /**
     * Avoid compilation warning & build failing. `@SuppressWarnings("deprecated")` does not work in this case
     */
    private static MessageSerializer getGryoV3D0Serializer() {
        try {
            return (MessageSerializer) Class.forName("org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV3d0").newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
