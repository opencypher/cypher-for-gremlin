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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.tinkerpop.gremlin.driver.RequestOptions;

public class CypherStatement {
    protected final String query;
    protected final Long timeout;
    protected final Map<String, ?> parameters;

    private CypherStatement(String query, Long timeout, Map<String, ?> parameters) {
        this.query = query;
        this.timeout = timeout;
        this.parameters = parameters;
    }

    /**
     * Create a new statement
     *
     * @param text query text
     */
    public static Simple create(String query) {
        return new Simple(query, null, Collections.emptyMap());
    }

    /**
     * Create a new statement with parameters
     *
     * @param text       query text
     * @param parameters statement parameters
     */
    public static Simple create(String query, Map<String, ?> parameters) {
        return new Simple(query, null, new HashMap<>(parameters));
    }

    /**
     * @return query
     */
    public String query() {
        return query;
    }

    /**
     * @return parameters
     */
    public Map<String, Object> parameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * @return timeout
     */
    public Optional<Long> timeout() {
        return Optional.ofNullable(timeout);
    }

    /**
     * Convert to Gremlin RequestOptions object
     *
     * @return RequestOptions
     */
    public RequestOptions requestOptions() {
        return timeout == null ? RequestOptions.EMPTY : RequestOptions.build().timeout(timeout).create();
    }

    private static abstract class Immutable<T extends Immutable<T>> extends CypherStatement {
        protected Immutable(String query, Long timeout, Map<String, ?> parameters) {
            super(query, timeout, parameters);
        }

        /**
         * @param newParameters the new statement's parameters
         * @return a new statement with updated parameters
         */
        public T withParameters(Map<String, Object> newParameters) {
            return instance(query, timeout, new HashMap<>(newParameters));
        }

        /**
         * @param name  parameter name
         * @param value parameter value
         * @return a new statement with added parameters
         */
        public T addParameter(String name, Object value) {
            HashMap<String, Object> newParameters = new HashMap<>(parameters);
            newParameters.put(name, value);
            return instance(query, timeout, newParameters);
        }

        /**
         * @param timeout query timeout
         * @param unit    time unit
         * @return a new statement with updated timeout
         */
        public T withTimeout(long timeout, TimeUnit unit) {
            return instance(query, unit.toMillis(timeout), parameters);
        }

        protected abstract T instance(String query, Long timeout, Map<String, ?> parameters);
    }

    public static class Simple extends Immutable<Simple> {
        protected Simple(String query, Long timeout, Map<String, ?> parameters) {
            super(query, timeout, parameters);
        }

        @Override
        protected Simple instance(String query, Long timeout, Map<String, ?> parameters) {
            return new Simple(query, timeout, parameters);
        }
    }

    public static class Submittable extends Immutable<Submittable> {
        private CypherGremlinClient client;

        protected Submittable(CypherGremlinClient client, String query, Long timeout, Map<String, ?> parameters) {
            super(query, timeout, parameters);
            this.client = client;
        }

        /**
         * Submits a Cypher statement asynchronously.
         *
         * @return Cypher-style results
         */
        public CompletableFuture<CypherResultSet> submit() {
            return client.submitAsync(this);
        }

        @Override
        protected Submittable instance(String query, Long timeout, Map<String, ?> parameters) {
            return new Submittable(client, query, timeout, parameters);
        }
    }
}
