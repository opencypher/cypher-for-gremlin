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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;

public class VeryUglyRetrier implements CypherGremlinClient {
    GroovyCypherGremlinClient delegate;

    public VeryUglyRetrier(Client client, Supplier<Translator<String, GroovyPredicate>> translatorSupplier) {
        delegate = new GroovyCypherGremlinClient(client, translatorSupplier);
    }

    @Override
    public CypherResultSet submit(String cypher) {
        return submit(cypher, new HashMap<>());
    }

    @Override
    public CypherResultSet submit(String cypher, Map<String, ?> parameters) {
        int retries = 3;
        RuntimeException exception = new RuntimeException();

        do {
            try {
                retries--;
                CypherResultSet cypherResultSet = delegate.submitAsync(cypher, parameters).join();
                List<Map<String, Object>> all = cypherResultSet.all();
                List<Result> converts = all.stream().map(Result::new).collect(Collectors.toList());
                return new CypherResultSet(converts.iterator());
            } catch (Exception e) {
                exception.addSuppressed(e); }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } while (retries > 0);

        throw exception;
    }

    @Override
    public CompletableFuture<CypherResultSet> submitAsync(String cypher, Map<String, ?> parameters) {
        return CompletableFuture.completedFuture(submit(cypher, parameters));
    }

    @Override
    public void close() {
        delegate.close();
    }
}
