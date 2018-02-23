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
package org.opencypher.gremlin.server;

import static java.util.Collections.singletonList;

import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerIoRegistryV3d0;


public final class EmbeddedGremlinServerFactory {
    private EmbeddedGremlinServerFactory() {
    }

    public static EmbeddedGremlinServer tinkerGraph() {
        return tinkerGraph(0);
    }

    public static EmbeddedGremlinServer tinkerGraph(int port) {
        return EmbeddedGremlinServer.builder()
            .port(port)
            .propertiesPath("../testware-common/src/main/resources/tinkergraph-empty.properties")
            .scriptPath("../testware-common/src/main/resources/generate-modern.groovy")
            .serializer(GryoMessageSerializerV3d0.class, singletonList(TinkerIoRegistryV3d0.class))
            .build();
    }
}
