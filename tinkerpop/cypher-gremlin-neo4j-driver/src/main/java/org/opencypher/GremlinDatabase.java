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
package org.opencypher;

import java.net.URI;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.neo4j.driver.v1.Driver;

/**
 * Creates {@link Driver drivers}, optionally letting you {@link #driver(URI, Config)} to configure them.
 */
public class GremlinDatabase {
    /**
     * Returns a driver for a Gremlin Server instance.
     *
     * @param uri the URL to a Gremlin Server instance
     */
    public static Driver driver(String uri) {
        return driver(URI.create(uri));
    }

    /**
     * Returns a driver for a Gremlin Server instance.
     *
     * @param uri the URL to a Gremlin Server instance
     */
    public static Driver driver(URI uri) {
        Cluster cluster = Cluster.build()
            .addContactPoint(uri.getHost())
            .port(uri.getPort())
            .create();

        return new GremlinServerDriver(cluster, Config.defaultConfig());
    }

    /**
     * Returns a driver for a Gremlin Server instance.
     *
     * @param cluster Gremlin Server cluster
     */
    public static Driver driver(Cluster cluster) {
        return new GremlinServerDriver(cluster, Config.defaultConfig());
    }

    /**
     * Returns a driver for a Gremlin Server instance.
     *
     * @param uri    the URL to a Gremlin Server instance
     * @param config configuration
     */
    public static Driver driver(String uri, Config config) {
        return driver(URI.create(uri), config);
    }

    /**
     * Returns a driver for a Gremlin Server instance.
     *
     * @param uri    the URL to a Gremlin Server instance
     * @param config configuration
     */
    public static Driver driver(URI uri, Config config) {
        Cluster cluster = Cluster.build()
            .addContactPoint(uri.getHost())
            .port(uri.getPort())
            .create();

        return new GremlinServerDriver(cluster, config);
    }

    /**
     * Returns a driver for a Gremlin Server instance.
     *
     * @param cluster Gremlin Server cluster
     * @param config  configuration
     */
    public static Driver driver(Cluster cluster, Config config) {
        return new GremlinServerDriver(cluster, config);
    }


}
