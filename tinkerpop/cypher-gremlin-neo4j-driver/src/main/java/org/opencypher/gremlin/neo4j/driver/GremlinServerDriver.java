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
package org.opencypher.gremlin.neo4j.driver;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.opencypher.gremlin.client.CypherGremlinClient;

class GremlinServerDriver implements GremlinDriver {
    private final Cluster cluster;
    private final GremlinServerInfo serverInfo;
    private final Config config;

    GremlinServerDriver(Cluster cluster, Config config) {
        this.cluster = cluster;
        this.config = config;
        serverInfo = new GremlinServerInfo(cluster.toString());
    }

    @Override
    public boolean isEncrypted() {
        return cluster.isSslEnabled();
    }

    @Override
    public Session session() {
        Client gremlinClient = cluster.connect();

        CypherGremlinClient cypherGremlinClient = config.translationEnabled()
            ? CypherGremlinClient.translating(gremlinClient, config.flavor())
            : CypherGremlinClient.plugin(gremlinClient);

        GremlinCypherValueConverter converter = new GremlinCypherValueConverter(config.isIgnoreIds());

        return new GremlinServerSession(serverInfo, cypherGremlinClient, converter);
    }

    @Override
    public void close() {
        cluster.close();
    }

    static class GremlinServerInfo implements ServerInfo {
        private final String address;

        GremlinServerInfo(String address) {
            this.address = address;
        }

        @Override
        public String address() {
            return address;
        }

        @Override
        public String version() {
            return null;
        }
    }
}
