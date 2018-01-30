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
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.jsr223.console.GremlinShellEnvironment;
import org.apache.tinkerpop.gremlin.jsr223.console.RemoteAcceptor;
import org.apache.tinkerpop.gremlin.jsr223.console.RemoteException;
import org.opencypher.gremlin.translation.Flavor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.tinkerpop.gremlin.console.jsr223.DriverRemoteAcceptor.NO_TIMEOUT;

/**
 * A {@link RemoteAcceptor} that takes a Cypher query from the console
 * and sends it to Gremlin Server over the standard Java driver.
 */

public class CypherRemoteAcceptor implements RemoteAcceptor {

    private DriverRemoteAcceptor delegate;
    private Client currentClient;
    private GremlinShellEnvironment shellEnvironment;
    private Map<String, String> aliases;
    private int timeout = NO_TIMEOUT;

    private QueryHandler queryHandler;

    private static final String TOKEN_TRANSLATE = "translate";
    private static final int DEFAULT_BATCH_SIZE = 64;

    CypherRemoteAcceptor(final GremlinShellEnvironment shellEnvironment) {
        this.shellEnvironment = shellEnvironment;
        delegate = new DriverRemoteAcceptor(shellEnvironment);
    }

    @Override
    public Object connect(List<String> args) throws RemoteException {
        Object connection = delegate.connect(args);
        currentClient = getField(delegate, "currentClient");
        aliases = getField(delegate, "aliases");
        queryHandler = configureQueryHandler(args);

        return connection;
    }

    static QueryHandler configureQueryHandler(List<String> args) {
        if (args.contains(TOKEN_TRANSLATE)) {
            Flavor flavor = Flavor.GREMLIN;
            int flavorParamIndex = args.indexOf(TOKEN_TRANSLATE) + 1;
            if (flavorParamIndex < args.size()) {
                flavor = Flavor.getFlavor(args.get(flavorParamIndex));
            }
            return new TranslatingQueryHandler(flavor);
        } else {
            return new SimpleQueryHandler();
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
        RequestMessage.Builder request = queryHandler.buildRequest(query)
            .add(Tokens.ARGS_BATCH_SIZE, DEFAULT_BATCH_SIZE);

        if (aliases != null && !aliases.isEmpty()) {
            request.addArg(Tokens.ARGS_ALIASES, aliases);
        }

        RequestMessage msg = currentClient.buildMessage(request).create();
        ResultSet rs = currentClient.submitAsync(msg).get();
        List<Result> results = timeout > NO_TIMEOUT ? rs.all().get(timeout, TimeUnit.MILLISECONDS) : rs.all().get();
        results = queryHandler.normalizeResults(results);

        return results;
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
