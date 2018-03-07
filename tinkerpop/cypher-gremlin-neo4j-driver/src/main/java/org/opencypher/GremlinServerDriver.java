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
package org.opencypher;

import static java.util.stream.Collectors.toList;
import static org.opencypher.GremlinCypherValueConverter.toRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.neo4j.driver.v1.summary.Notification;
import org.neo4j.driver.v1.summary.Plan;
import org.neo4j.driver.v1.summary.ProfiledPlan;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.neo4j.driver.v1.summary.StatementType;
import org.neo4j.driver.v1.summary.SummaryCounters;
import org.neo4j.driver.v1.types.TypeSystem;
import org.neo4j.driver.v1.util.Function;
import org.opencypher.gremlin.client.CypherGremlinClient;


class GremlinServerDriver implements Driver {
    private final Cluster cluster;
    private final ServerInfo serverInfo;
    private final Config config;


    GremlinServerDriver(Cluster cluster, Config config) {
        this.cluster = cluster;
        this.serverInfo = new GremlinServerInfo(cluster.toString());
        this.config = config;
    }

    @Override
    public boolean isEncrypted() {
        return cluster.isSslEnabled();
    }

    @Override
    public Session session() {
        Client gremlinClient = cluster.connect();

        CypherGremlinClient cypherGremlinClient = config.translationEnabled()
            ? CypherGremlinClient.translating(gremlinClient, config.flavor())
            : CypherGremlinClient.plugin(gremlinClient);

        return new GremlinServerSession(serverInfo, cypherGremlinClient);
    }

    @Override
    public Session session(AccessMode mode) {
        return notSupported("Transactions");
    }

    @Override
    public Session session(String bookmark) {
        return notSupported("Transactions");
    }

    @Override
    public Session session(AccessMode mode, String bookmark) {
        return notSupported("Transactions");
    }

    @Override
    public Session session(Iterable<String> bookmarks) {
        return notSupported("Transactions");
    }

    @Override
    public Session session(AccessMode mode, Iterable<String> bookmarks) {
        return notSupported("Transactions");
    }

    @Override
    public void close() {
        cluster.close();
    }

    private static class GremlinServerSession implements Session {
        private ServerInfo serverInfo;
        private final CypherGremlinClient client;
        private boolean open = true;

        private GremlinServerSession(ServerInfo serverInfo, CypherGremlinClient client) {
            this.serverInfo = serverInfo;
            this.client = client;
        }

        @Override
        public Transaction beginTransaction() {
            return notSupported("Transactions");
        }

        @Override
        @SuppressWarnings("deprecation")
        public Transaction beginTransaction(String bookmark) {
            return notSupported("Transactions");
        }

        @Override
        public <T> T readTransaction(TransactionWork<T> work) {
            return notSupported("Transactions");
        }

        @Override
        public <T> T writeTransaction(TransactionWork<T> work) {
            return notSupported("Transactions");
        }

        @Override
        public String lastBookmark() {
            return notSupported("Transactions");
        }

        @Override
        @SuppressWarnings("deprecation")
        public void reset() {
            notSupported("Transactions");
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
            Iterator<Map<String, Object>> iterator = client.submit(statement.text(), statement.parameters().asMap()).iterator();
            return new GremlinServerStatementResult(serverInfo, statement, iterator);
        }

        @Override
        public TypeSystem typeSystem() {
            return notSupported("TypeSystem");
        }
    }

    static class GremlinServerStatementResult implements StatementResult {
        private final PeekingIterator<Map<String, Object>> iterator;
        private final ServerInfo serverInfo;
        private final Statement statement;


        GremlinServerStatementResult(ServerInfo serverInfo, Statement statement, Iterator<Map<String, Object>> iterator) {
            this.iterator = new PeekingIterator<>(iterator);
            this.serverInfo = serverInfo;
            this.statement = statement;
        }


        @Override
        public List<String> keys() {
            return peek().keys();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Record next() {
            return toRecord(iterator.next());
        }

        @Override
        public Record single() throws NoSuchRecordException {
            if (!iterator.hasNext()) {
                throw new NoSuchRecordException("Cannot retrieve a single record, because this result is empty.");
            }

            Record next = next();

            if (iterator.hasNext()) {
                throw new NoSuchRecordException("Expected a result with a single record, but this result contains at least one more. " +
                    "Ensure your query returns only one record, or use `first` instead of `single` if " +
                    "you do not care about the number of records in the result.");
            }

            return next;
        }

        @Override
        public Record peek() {
            return toRecord(iterator.peek());
        }

        @Override
        public List<Record> list() {
            ArrayList<Record> list = new ArrayList<>();
            iterator.forEachRemaining(e -> list.add(toRecord(e)));
            return list;
        }

        @Override
        public <T> List<T> list(Function<Record, T> mapFunction) {
            return list().stream()
                .map(mapFunction::apply)
                .collect(toList());
        }

        @Override
        public ResultSummary consume() {
            list();
            return new GremlinServerResultSummary(statement, serverInfo);
        }

        @Override
        public ResultSummary summary() {
            return new GremlinServerResultSummary(statement, serverInfo);
        }
    }

    private static class GremlinServerResultSummary implements ResultSummary {
        private Statement statement;
        private ServerInfo serverInfo;

        GremlinServerResultSummary(Statement statement, ServerInfo serverInfo) {
            this.statement = statement;
            this.serverInfo = serverInfo;
        }

        @Override
        public Statement statement() {
            return statement;
        }

        @Override
        public SummaryCounters counters() {
            return notSupported("SummaryCounters");
        }

        @Override
        public StatementType statementType() {
            return notSupported("StatementType");
        }

        @Override
        public boolean hasPlan() {
            return false;
        }

        @Override
        public boolean hasProfile() {
            return false;
        }

        @Override
        public Plan plan() {
            return null;
        }

        @Override
        public ProfiledPlan profile() {
            return null;
        }

        @Override
        public List<Notification> notifications() {
            return new ArrayList<>();
        }

        @Override
        public long resultAvailableAfter(TimeUnit unit) {
            return 0;
        }

        @Override
        public long resultConsumedAfter(TimeUnit unit) {
            return 0;
        }

        @Override
        public ServerInfo server() {
            return serverInfo;
        }
    }

    static class GremlinServerInfo implements ServerInfo {
        private final String address;

        GremlinServerInfo(String address) {
            this.address = address;
        }

        @Override
        public String address() {
            return address;
        }

        @Override
        public String version() {
            return null;
        }
    }

    static class PeekingIterator<E> implements Iterator<E> {
        private final Iterator<E> iterator;
        private E peeked = null;

        PeekingIterator(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        private boolean hasPeeked() {
            return peeked != null;
        }

        @Override
        public boolean hasNext() {
            return hasPeeked() || iterator.hasNext();
        }

        @Override
        public E next() {
            if (!hasPeeked()) {
                return iterator.next();
            }
            E result = peeked;
            peeked = null;
            return result;
        }

        @Override
        public void remove() {
            notSupported("Not supported");
        }

        public E peek() {
            if (!hasPeeked()) {
                peeked = iterator.next();
            }
            return peeked;
        }
    }

    private static <T> T notSupported(String feature) {
        throw new UnsupportedOperationException(feature + " is currently not supported GfoG adapter");
    }
}
