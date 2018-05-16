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
package org.opencypher.gremlin.neo4j.driver;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.neo4j.driver.v1.types.TypeSystem;
import org.opencypher.gremlin.client.CypherGremlinClient;

class GremlinServerSession implements Session {
    private ServerInfo serverInfo;
    private final CypherGremlinClient client;
    private boolean open = true;

    GremlinServerSession(ServerInfo serverInfo, CypherGremlinClient client) {
        this.serverInfo = serverInfo;
        this.client = client;
    }

    @Override
    public Transaction beginTransaction() {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    @Override
    @SuppressWarnings("deprecation")
    public Transaction beginTransaction(String bookmark) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    @Override
    public <T> T readTransaction(TransactionWork<T> work) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    @Override
    public <T> T writeTransaction(TransactionWork<T> work) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    @Override
    public String lastBookmark() {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reset() {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
        client.close();
    }

    @Override
    public StatementResult run(String statementTemplate, Value parameters) {
        return run(statementTemplate, parameters.asMap());
    }

    @Override
    public StatementResult run(String statementTemplate, Record statementParameters) {
        return run(statementTemplate, statementParameters.asMap());
    }

    @Override
    public StatementResult run(String statementTemplate) {
        return run(statementTemplate, new HashMap<>());
    }

    @Override
    public StatementResult run(String statementTemplate, Map<String, Object> statementParameters) {
        return run(new Statement(statementTemplate, statementParameters));
    }

    @Override
    public StatementResult run(Statement statement) {
        HashMap<String, Object> serializableMap = new HashMap<>(statement.parameters().asMap());
        Iterator<Map<String, Object>> iterator = client.submit(statement.text(), serializableMap).iterator();
        return new GremlinServerStatementResult(serverInfo, statement, iterator);
    }

    @Override
    public TypeSystem typeSystem() {
        throw new UnsupportedOperationException("TypeSystem is currently not supported");
    }
}
