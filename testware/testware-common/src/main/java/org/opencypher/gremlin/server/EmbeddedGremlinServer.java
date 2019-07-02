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
package org.opencypher.gremlin.server;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.structure.io.IoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmbeddedGremlinServer {

    private Logger logger = LoggerFactory.getLogger(EmbeddedGremlinServer.class);
    private GremlinServer gremlinServer;
    private final Settings settings;

    private EmbeddedGremlinServer(Settings settings) {
        this.settings = settings;
    }

    public void start() {
        if (gremlinServer != null) {
            throw new IllegalStateException("EmbeddedGremlinServer already started!");
        }
        try {
            gremlinServer = new GremlinServer(settings);
            gremlinServer.start().join();
            logger.info("EmbeddedGremlinServer started (port {})", getPort());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (gremlinServer == null) {
            throw new IllegalStateException("EmbeddedGremlinServer not started!");
        }
        logger.info("Shutting down EmbeddedGremlinServer");
        gremlinServer.stop();
        gremlinServer = null;
    }

    public int getPort() {
        return settings.port;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int port;
        private Map<String, String> graphs = new HashMap<>();
        private String scriptPath;
        public Map<String, Map<String, Object>> plugins = new HashMap<>();
        private Multimap<Class<? extends MessageSerializer>, Class<? extends IoRegistry>> serializers =
            HashMultimap.create();

        private Builder() {
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder propertiesPath(String graph, String propertiesPath) {
            this.graphs.put(graph, propertiesPath);
            return this;
        }

        public Builder scriptPath(String scriptPath) {
            this.scriptPath = scriptPath;
            return this;
        }

        public Builder serializer(Class<? extends MessageSerializer> serializer,
                                  List<Class<? extends IoRegistry>> ioRegistries) {
            serializers.putAll(serializer, ioRegistries);
            return this;
        }

        public Builder addPlugin(String name, Map<String, Object> properties) {
            plugins.put(name, properties);
            return this;
        }

        public EmbeddedGremlinServer build() {
            graphs.values().forEach(path -> checkFile("propertiesPath", path));
            checkFile("scriptPath", scriptPath);

            Settings settings = new Settings();
            settings.port = getFreePort();
            settings.graphs = graphs;

            Settings.ScriptEngineSettings gremlinGroovy = settings.scriptEngines.get("gremlin-groovy");
            gremlinGroovy.imports.add("java.lang.Math");
            gremlinGroovy.plugins.put("org.apache.tinkerpop.gremlin.server.jsr223.GremlinServerGremlinPlugin", emptyMap());
            gremlinGroovy.plugins.put("org.apache.tinkerpop.gremlin.tinkergraph.jsr223.TinkerGraphGremlinPlugin", emptyMap());
            gremlinGroovy.plugins.put("org.opencypher.gremlin.server.jsr223.CypherPlugin", emptyMap());
            gremlinGroovy.plugins.put("org.apache.tinkerpop.gremlin.jsr223.ScriptFileGremlinPlugin", singletonMap("files", singletonList(scriptPath)));
            gremlinGroovy.plugins.putAll(plugins);
            gremlinGroovy.staticImports.add("java.lang.Math.PI");

            settings.serializers = new ArrayList<>(settings.serializers);
            serializers.asMap().forEach((serializer, ioRegistries) -> {
                Settings.SerializerSettings serializerSettings = new Settings.SerializerSettings();
                serializerSettings.className = serializer.getCanonicalName();
                serializerSettings.config = singletonMap("ioRegistries", ioRegistries.stream()
                    .map(Class::getCanonicalName)
                    .collect(toList()));
                settings.serializers.add(serializerSettings);
            });

            return new EmbeddedGremlinServer(settings);
        }

        private static void checkFile(String key, String path) {
            Preconditions.checkNotNull(path, key + " is required");
            File file = new File(path);
            Preconditions.checkState(file.exists(), format("file %s doesn't exist", file.getAbsolutePath()));
        }

        private int getFreePort() {
            try (ServerSocket socket = new ServerSocket(port)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
