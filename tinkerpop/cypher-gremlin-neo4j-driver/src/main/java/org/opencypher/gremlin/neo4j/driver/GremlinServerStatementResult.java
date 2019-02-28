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
package org.opencypher.gremlin.neo4j.driver;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.neo4j.driver.v1.util.Function;

class GremlinServerStatementResult implements StatementResult {
    private final PeekingIterator<Map<String, Object>> iterator;
    private final ServerInfo serverInfo;
    private final Statement statement;
    private final GremlinCypherValueConverter converter;

    GremlinServerStatementResult(ServerInfo serverInfo,
                                 Statement statement,
                                 Iterator<Map<String, Object>> iterator,
                                 GremlinCypherValueConverter converter) {
        this.iterator = new PeekingIterator<>(iterator);
        this.serverInfo = serverInfo;
        this.statement = statement;
        this.converter = converter;
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
        return converter.toRecord(iterator.next());
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
        return converter.toRecord(iterator.peek());
    }

    @Override
    public List<Record> list() {
        ArrayList<Record> list = new ArrayList<>();
        iterator.forEachRemaining(e -> list.add(converter.toRecord(e)));
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
