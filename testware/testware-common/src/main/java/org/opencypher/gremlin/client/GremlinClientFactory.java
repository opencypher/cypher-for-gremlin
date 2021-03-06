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

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;

public final class GremlinClientFactory {
    public static final String TOKEN_TRANSLATE = "translate";
    public static final String TOKEN_CONFIG = "configPath";

    private GremlinClientFactory() {
    }

    public static Client create(int port) {
        return create(port, Serializers.GRAPHBINARY_V1D0.simpleInstance());
    }

    public static Client create(int port, MessageSerializer serializer) {
        return Cluster.build()
            .port(port)
            .serializer(serializer)
            .create()
            .connect();
    }
}
