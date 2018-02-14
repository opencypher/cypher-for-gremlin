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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.opencypher.GremlinServerDriver.GremlinServerInfo;

public class GremlinServerStatementResultTest {
    static final Statement statement = new Statement("RETURN 1;");
    static final GremlinServerInfo serverInfo = new GremlinServerInfo("localhost:1111");
    static final String KEY1 = "key1";
    static final String KEY2 = "key2";

    @Test
    public void create() {
        List<Map<String, Object>> results = Arrays.asList(
            getRow(1),
            getRow(2),
            getRow(3)
        );

        StatementResult statementResult = new GremlinServerDriver.GremlinServerStatementResult(serverInfo, statement, results.iterator());

        assertThat(statementResult.keys()).containsExactly(KEY1, KEY2);
        assertThat(statementResult.peek().get(KEY1).asInt()).isEqualTo(1);
        assertThat(statementResult.hasNext()).isTrue();
        assertThat(statementResult.next().get(KEY1).asInt()).isEqualTo(1);
        assertThat(statementResult.hasNext()).isTrue();
        assertThat(statementResult.next().get(KEY1).asInt()).isEqualTo(2);
        assertThat(statementResult.hasNext()).isTrue();

        //see org.neo4j.driver.internal.InternalStatementResult#list()
        assertThat(statementResult.list()).hasSize(1);
        assertThat(statementResult.hasNext()).isFalse();

        assertThat(statementResult.summary().server()).isEqualTo(serverInfo);
    }

    @Test
    public void consume() {
        List<Map<String, Object>> results = Arrays.asList(
            getRow(1),
            getRow(2)
        );

        StatementResult statementResult = new GremlinServerDriver.GremlinServerStatementResult(serverInfo, statement, results.iterator());

        assertThat(statementResult.hasNext()).isTrue();
        assertThat(statementResult.consume().server()).isEqualTo(serverInfo);
        assertThat(statementResult.hasNext()).isFalse();
    }

    @Test
    public void map() {
        List<Map<String, Object>> results = Arrays.asList(
            getRow(1),
            getRow(2)
        );

        StatementResult statementResult = new GremlinServerDriver.GremlinServerStatementResult(serverInfo, statement, results.iterator());

        assertThat(statementResult.list(r -> r.get(KEY2).asString()))
            .containsExactly("value1", "value2");
    }

    @Test
    public void single() {
        List<Map<String, Object>> results = Arrays.asList(getRow(1));
        StatementResult statementResult = new GremlinServerDriver.GremlinServerStatementResult(serverInfo, statement, results.iterator());

        assertThat(statementResult.single().get(KEY1).asInt()).isEqualTo(1);
    }

    @Test(expected = NoSuchRecordException.class)
    public void singleMore() {
        List<Map<String, Object>> results = Arrays.asList(getRow(1), getRow(2));
        StatementResult statementResult = new GremlinServerDriver.GremlinServerStatementResult(serverInfo, statement, results.iterator());

        statementResult.single();
    }

    @Test(expected = NoSuchRecordException.class)
    public void singleZero() {
        StatementResult statementResult = new GremlinServerDriver.GremlinServerStatementResult(serverInfo,
            statement, new ArrayList<Map<String, Object>>().iterator());

        statementResult.single();
    }

    private Map<String, Object> getRow(int i) {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY1, i);
        map.put(KEY2, "value" + i);
        return map;
    }
}
