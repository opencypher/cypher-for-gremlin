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

import static java.util.Collections.emptyMap;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

/**
 * This is a convenience wrapper around a Gremlin {@link Client}
 * that configures the provided {@link Client} to be able to send Cypher queries
 * to a Gremlin Server.
 */
public interface CypherGremlinClient extends Closeable {

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to a remote Gremlin Server with Cypher plugin.
     *
     * @param client Gremlin client
     * @return Cypher-enabled client
     */
    static CypherGremlinClient plugin(Client client) {
        return new OpProcessorCypherGremlinClient(client);
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin-Groovy.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client Gremlin client
     * @return Cypher-enabled client
     */
    static CypherGremlinClient translating(Client client) {
        return translating(client, TranslatorFlavor.gremlinServer());
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin-Groovy.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client Gremlin client
     * @param flavor translation flavor
     * @return Cypher-enabled client
     */
    static CypherGremlinClient translating(Client client, TranslatorFlavor flavor) {
        return translating(client, () -> Translator.builder().gremlinGroovy().build(flavor));
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin-Groovy.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client             Gremlin client
     * @param translatorSupplier translator configuration supplier
     * @return Cypher-enabled client
     */
    static CypherGremlinClient translating(Client client, Supplier<Translator<String, GroovyPredicate>> translatorSupplier) {
        return new GroovyCypherGremlinClient(client, translatorSupplier);
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin bytecode.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client Gremlin client
     * @return Cypher-enabled client
     */
    static CypherGremlinClient bytecode(Client client) {
        return bytecode(client, TranslatorFlavor.gremlinServer());
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin bytecode.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client Gremlin client
     * @param flavor translation flavor
     * @return Cypher-enabled client
     */
    static CypherGremlinClient bytecode(Client client, TranslatorFlavor flavor) {
        return bytecode(client, () -> Translator.builder().bytecode().build(flavor));
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin bytecode.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     *
     * @param client             Gremlin client
     * @param translatorSupplier translator configuration supplier
     * @return Cypher-enabled client
     */
    static CypherGremlinClient bytecode(Client client, Supplier<Translator<Bytecode, P>> translatorSupplier) {
        return new BytecodeCypherGremlinClient(client, translatorSupplier);
    }

    /**
     * Creates a {@link CypherGremlinClient} that executes Cypher queries
     * directly on the configured {@link GraphTraversalSource}.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread.
     * Graph traversal execution is not synchronized.
     *
     * @param gts source of {@link GraphTraversal} to translate to
     * @return Cypher-enabled client
     */
    static CypherGremlinClient inMemory(GraphTraversalSource gts) {
        return new InMemoryCypherGremlinClient(gts);
    }

    /**
     * Creates a {@link CypherGremlinClient} that can send Cypher queries
     * to any Gremlin Server or a compatible graph database as Gremlin-Groovy.
     * <p>
     * Cypher to Gremlin translation is done on the client's thread,
     * before sending the query to Gremlin Server.
     * <p>
     * Difference with {@link #translating(Client, TranslatorFlavor)} is that on request client retrieves and stores
     * the entire result stream. See {@link ResultSet#all()}.
     * <p>
     * This is not optimal for large result sets as the results will be held in memory at once.
     * Use only for compatibility reasons.
     *
     * @see #translating(Client, TranslatorFlavor)
     * @param client Gremlin client
     * @param flavor translation flavor
     * @return Cypher-enabled client
     */
    static CypherGremlinClient retrieving(Client client, TranslatorFlavor flavor) {
        return new SyncGroovyCypherGremlinClient(client, () -> Translator.builder().gremlinGroovy().build(flavor));
    }

    /**
     * Closes the underlying Gremlin client.
     */
    @Override
    void close();

    /**
     * Submits a Cypher query.
     *
     * @param cypher query text
     * @return Cypher-style results
     */
    default CypherResultSet submit(String cypher) {
        return submitAsync(cypher, emptyMap()).join();
    }

    /**
     * Submits a Cypher query.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @return Cypher-style results
     */
    default CypherResultSet submit(String cypher, Map<String, ?> parameters) {
        return submitAsync(cypher, parameters).join();
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher query text
     * @return Cypher-style results
     */
    default CompletableFuture<CypherResultSet> submitAsync(String cypher) {
        return submitAsync(cypher, emptyMap());
    }

    /**
     * Submits a Cypher query asynchronously.
     *
     * @param cypher     query text
     * @param parameters query parameters
     * @return Cypher-style results
     */
    CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters);
}
