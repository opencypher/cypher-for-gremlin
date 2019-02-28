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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.summary.Notification;
import org.neo4j.driver.v1.summary.Plan;
import org.neo4j.driver.v1.summary.ProfiledPlan;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.summary.ServerInfo;
import org.neo4j.driver.v1.summary.StatementType;
import org.neo4j.driver.v1.summary.SummaryCounters;

class GremlinServerResultSummary implements ResultSummary {
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
        throw new UnsupportedOperationException("SummaryCounters is currently not supported");
    }

    @Override
    public StatementType statementType() {
        throw new UnsupportedOperationException("StatementType is currently not supported");
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
