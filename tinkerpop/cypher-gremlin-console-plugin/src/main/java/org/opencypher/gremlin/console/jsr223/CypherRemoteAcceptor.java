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
package org.opencypher.gremlin.console.jsr223;

import org.apache.tinkerpop.gremlin.console.jsr223.DriverRemoteAcceptor;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.jsr223.console.GremlinShellEnvironment;
import org.apache.tinkerpop.gremlin.jsr223.console.RemoteAcceptor;
import org.apache.tinkerpop.gremlin.jsr223.console.RemoteException;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.client.CypherGremlinClientFactory;
import org.opencypher.gremlin.translation.Flavor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static org.apache.tinkerpop.gremlin.console.jsr223.DriverRemoteAcceptor.NO_TIMEOUT;

/**
 * A {@link RemoteAcceptor} that takes a Cypher query from the console
 * and sends it to Gremlin Server over the standard Java driver.
 */

public class CypherRemoteAcceptor implements RemoteAcceptor {

    private DriverRemoteAcceptor delegate;
    private GremlinShellEnvironment shellEnvironment;
    private int timeout = NO_TIMEOUT;

    private CypherGremlinClient client;

    private static final String TOKEN_TRANSLATE = "translate";

    CypherRemoteAcceptor(final GremlinShellEnvironment shellEnvironment) {
        this.shellEnvironment = shellEnvironment;
        delegate = new DriverRemoteAcceptor(shellEnvironment);
    }

    @Override
    public Object connect(List<String> args) throws RemoteException {
        Object result = delegate.connect(args);
        Client gremlinClient = getField(delegate, "currentClient");
        Map<String, String> aliases = getField(delegate, "aliases");
        if (aliases != null) {
            gremlinClient = gremlinClient.alias(aliases);
        }
        client = configureClient(gremlinClient, args);
        return result;
    }

    private static CypherGremlinClient configureClient(Client gremlinClient, List<String> args) {
        if (args.contains(TOKEN_TRANSLATE)) {
            Flavor flavor = Flavor.GREMLIN;
            int flavorParamIndex = args.indexOf(TOKEN_TRANSLATE) + 1;
            if (flavorParamIndex < args.size()) {
                flavor = Flavor.getFlavor(args.get(flavorParamIndex));
            }
            return CypherGremlinClientFactory.translating(gremlinClient, flavor);
        } else {
            return CypherGremlinClientFactory.plugin(gremlinClient);
        }
    }

    @Override
    public Object configure(List<String> args) throws RemoteException {
        Object configured = delegate.configure(args);
        timeout = getField(delegate, "timeout");

        return configured;
    }

    @Override
    public Object submit(List<String> args) throws RemoteException {
        String line = String.join(" ", args);
        line = DriverRemoteAcceptor.getScript(line, shellEnvironment);
        try {
            final List<Result> resultSet = send(line);
            shellEnvironment.setVariable(RESULT, resultSet);
            return resultSet.stream().map(Result::getObject).iterator();
        } catch (Exception ex) {
            throw new RemoteException(ex);
        }

    }

    private List<Result> send(String query) throws Exception {
        CompletableFuture<List<Map<String, Object>>> resultsFuture = client.submitAsync(query);
        List<Map<String, Object>> results = (timeout > NO_TIMEOUT) ?
            resultsFuture.get(timeout, TimeUnit.MILLISECONDS) :
            resultsFuture.get();
        return results.stream()
            .map(Result::new)
            .collect(toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object obj, String fieldName) throws RemoteException {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
