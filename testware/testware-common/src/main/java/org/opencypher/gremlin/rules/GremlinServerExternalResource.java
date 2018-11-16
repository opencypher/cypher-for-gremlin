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
package org.opencypher.gremlin.rules;

import static com.google.common.base.Strings.emptyToNull;
import static org.opencypher.gremlin.client.GremlinClientFactory.TOKEN_TRANSLATE;

import java.util.Optional;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.util.function.ThrowingConsumer;
import org.assertj.core.util.Strings;
import org.junit.rules.ExternalResource;
import org.opencypher.gremlin.client.CypherGremlinClient;
import org.opencypher.gremlin.client.GremlinClientFactory;
import org.opencypher.gremlin.server.EmbeddedGremlinServer;
import org.opencypher.gremlin.server.EmbeddedGremlinServerFactory;
import org.opencypher.gremlin.test.TestCommons;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GremlinServerExternalResource extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(GremlinServerExternalResource.class);

    private EmbeddedGremlinServer gremlinServer;
    private Client gremlinClient;
    private CypherGremlinClient cypherGremlinClient;
    private Supplier<EmbeddedGremlinServer> serverSupplier;
    private ThrowingConsumer<CypherGremlinClient> setup;

    public GremlinServerExternalResource() {
        this((o) -> o.submit(TestCommons.DELETE_ALL));
    }

    public GremlinServerExternalResource(Supplier<EmbeddedGremlinServer> serverSupplier) {
        this.serverSupplier = serverSupplier;
        this.setup = (o) -> {
        };
    }

    public GremlinServerExternalResource(ThrowingConsumer<CypherGremlinClient> setup) {
        this.serverSupplier = (EmbeddedGremlinServerFactory::tinkerGraph);
        this.setup = setup;
    }

    @Override
    public void before() throws Throwable {
        gremlinClient = configuredGremlinClient();
        cypherGremlinClient = configuredCypherGremlinClient();
        setup.accept(cypherGremlinClient);
    }

    private Client configuredGremlinClient() throws Exception {
        Client gremlinClient;

        String configPath = System.getProperty(GremlinClientFactory.TOKEN_CONFIG);
        if (!Strings.isNullOrEmpty(configPath)) {
            logger.info("Running tests using configuration " + configPath);
            gremlinClient = Cluster.open(configPath).connect();
        } else {
            logger.info("Running tests using embeded TinkerGraph");
            gremlinServer = serverSupplier.get();
            gremlinServer.start();
            int port = gremlinServer.getPort();
            gremlinClient = GremlinClientFactory.create(port);
        }

        return gremlinClient;
    }

    private CypherGremlinClient configuredCypherGremlinClient() {
        String translate = emptyToNull(System.getProperty(TOKEN_TRANSLATE));
        String clientName = Optional.ofNullable(translate).orElse("traversal");
        switch (clientName) {
            case "traversal":
                return CypherGremlinClient.plugin(gremlinClient);
            case "gremlin":
                return CypherGremlinClient.translating(gremlinClient, () -> Translator.builder()
                    .gremlinGroovy()
                    .enableCypherExtensions()
                    .build());
            case "vanilla":
                return CypherGremlinClient.translating(gremlinClient, () -> Translator.builder()
                    .gremlinGroovy()
                    .build());
            case "bytecode":
                return CypherGremlinClient.bytecode(gremlinClient.alias("g"), () -> Translator.builder()
                    .bytecode()
                    .enableCypherExtensions()
                    .build());
            case "cosmosdb":
                return CypherGremlinClient.translating(gremlinClient, () -> Translator.builder()
                    .gremlinGroovy()
                    .build(TranslatorFlavor.cosmosDb()));
            case "neptune":
                return CypherGremlinClient.translating(gremlinClient, () -> Translator.builder()
                    .gremlinGroovy()
                    .inlineParameters()
                    .enableMultipleLabels()
                    .build(TranslatorFlavor.neptune()));
            default:
                throw new IllegalArgumentException("Unknown name: " + clientName);
        }
    }

    @Override
    public void after() {
        cypherGremlinClient.close();
        if (gremlinServer != null) {
            gremlinServer.stop();
        }
    }

    public int getPort() {
        return gremlinClient.getCluster().getPort();
    }

    public Client gremlinClient() {
        return gremlinClient;
    }

    public CypherGremlinClient cypherGremlinClient() {
        return cypherGremlinClient;
    }
}
