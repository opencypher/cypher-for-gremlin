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

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Host;
import org.apache.tinkerpop.gremlin.driver.LoadBalancingStrategy;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.exception.ConnectionException;
import org.apache.tinkerpop.gremlin.driver.exception.ResponseException;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseResult;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatus;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteTraversal;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteTraversalSideEffects;
import org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerGremlinV1d0;
import org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerGremlinV2d0;
import org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV1d0;
import org.apache.tinkerpop.gremlin.driver.ser.GraphSONMessageSerializerV2d0;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV1d0;
import org.apache.tinkerpop.gremlin.driver.ser.GryoMessageSerializerV3d0;
import org.apache.tinkerpop.gremlin.driver.ser.JsonBuilderGryoSerializer;
import org.apache.tinkerpop.gremlin.driver.ser.MessageTextSerializer;
import org.apache.tinkerpop.gremlin.driver.ser.SerTokens;
import org.apache.tinkerpop.gremlin.driver.ser.SerializationException;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.jsr223.AbstractGremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.DefaultImportCustomizer;
import org.apache.tinkerpop.gremlin.jsr223.ImportCustomizer;
import org.apache.tinkerpop.gremlin.jsr223.console.ConsoleCustomizer;
import org.opencypher.gremlin.client.CypherTraversalSource;

/**
 * Gremlin Console Plugin that allows to send Cypher queries from Gremlin Console to Gremlin Server.
 *
 * @see CypherRemoteAcceptor
 */
public abstract class CypherPlugin extends AbstractGremlinPlugin {

    private static final ImportCustomizer imports = DefaultImportCustomizer.build()
        .addClassImports(Cluster.class,
            Client.class,
            Host.class,
            LoadBalancingStrategy.class,
            MessageSerializer.class,
            Result.class,
            ResultSet.class,
            Tokens.class,
            ConnectionException.class,
            ResponseException.class,
            RequestMessage.class,
            ResponseMessage.class,
            ResponseResult.class,
            ResponseStatus.class,
            ResponseStatusCode.class,
            GraphSONMessageSerializerGremlinV1d0.class,
            GraphSONMessageSerializerGremlinV2d0.class,
            GraphSONMessageSerializerV1d0.class,
            GraphSONMessageSerializerV2d0.class,
            GryoMessageSerializerV1d0.class,
            GryoMessageSerializerV3d0.class,
            JsonBuilderGryoSerializer.class,
            MessageTextSerializer.class,
            SerializationException.class,
            Serializers.class,
            SerTokens.class,
            DriverRemoteConnection.class,
            DriverRemoteTraversal.class,
            DriverRemoteTraversalSideEffects.class,
            CypherTraversalSource.class
        ).create();

    CypherPlugin(String name, ConsoleCustomizer customizer) {
        super(name, imports, customizer);
    }

    @Override
    public boolean requireRestart() {
        return true;
    }
}
