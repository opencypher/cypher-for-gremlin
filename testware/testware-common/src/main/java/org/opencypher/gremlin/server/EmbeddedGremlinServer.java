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

import com.google.common.base.Preconditions;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

public final class EmbeddedGremlinServer {

    private final GremlinServer gremlinServer;

    private final int port;

    private String serverName = getClass().getSimpleName();

    private EmbeddedGremlinServer(Builder builder) {
        Settings settings = new Settings();
        if (builder.port != null) {
            port = builder.port;
        } else {
            port = findAvailablePort();
        }

        settings.port = port;
        settings.graphs = singletonMap("graph", checkFile(builder.propertiesPath));

        Settings.ScriptEngineSettings gremlinGroovy = settings.scriptEngines.get("gremlin-groovy");
        gremlinGroovy.imports.add("java.lang.Math");
        gremlinGroovy.plugins.put("org.apache.tinkerpop.gremlin.server.jsr223.GremlinServerGremlinPlugin", emptyMap());
        gremlinGroovy.plugins.put("org.apache.tinkerpop.gremlin.tinkergraph.jsr223.TinkerGraphGremlinPlugin", emptyMap());
        gremlinGroovy.plugins.put("org.opencypher.gremlin.server.jsr223.CypherPlugin", emptyMap());
        gremlinGroovy.plugins.put("org.apache.tinkerpop.gremlin.jsr223.ScriptFileGremlinPlugin", singletonMap("files", singletonList(builder.scriptPath)));
        gremlinGroovy.staticImports.add("java.lang.Math.PI");

        if (builder.updater != null) {
            builder.updater.accept(settings);
        }
        gremlinServer = new GremlinServer(settings);
    }

    private String checkFile(String fileName) {
        File file = new File(fileName);
        Preconditions.checkState(file.exists(), format("File %s doesn't exist", file.getAbsolutePath()));
        return fileName;
    }

    public void start() {
        try {
            gremlinServer.start().get();
            System.out.println(format(serverName + " started (port %s)", port));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> stop() {
        System.out.println("Shutting down " + serverName);

        // do not wait for server being stopped (partial tests parallelization)
        return gremlinServer.stop();
    }

    public int getPort() {
        return port;
    }

    private static int findAvailablePort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int result = server.getLocalPort();
            server.close();
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Integer port;
        private String propertiesPath;
        private String scriptPath;
        private Consumer<Settings> updater;

        private Builder() {
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder propertiesPath(String propertiesPath) {
            this.propertiesPath = propertiesPath;
            return this;
        }

        public Builder scriptPath(String scriptPath) {
            this.scriptPath = scriptPath;
            return this;
        }

        public Builder settingsUpdater(Consumer<Settings> updater) {
            this.updater = updater;
            return this;
        }

        public EmbeddedGremlinServer build() {
            Preconditions.checkNotNull(propertiesPath, "propertiesPath is required");
            Preconditions.checkNotNull(scriptPath, "scriptPath is required");
            return new EmbeddedGremlinServer(this);
        }
    }
}
