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
package org.opencypher.gremlin.client;


import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class CypherStatementTest {
    public static final String QUERY = "MATCH (n) RETURN n";

    @Test
    public void statement() {
        CypherStatement.Simple statement = CypherStatement.create(QUERY)
            .withParameters(singletonMap("a", 1))
            .addParameter("b", 2)
            .addParameter("c", 3)
            .withTimeout(10, TimeUnit.SECONDS);

        assertThat(statement.query()).isEqualTo(QUERY);
        assertThat(statement.parameters()).containsExactly(entry("a", 1), entry("b", 2), entry("c", 3));
        assertThat(statement.timeout()).hasValue(10000L);
        assertThat(statement.requestOptions().getTimeout()).hasValue(10000L);
    }

    @Test
    public void statementImmutability() {
        CypherStatement.Simple statement0 = CypherStatement.create(QUERY, singletonMap("d", 4));
        CypherStatement.Simple statement1 = statement0.withTimeout(10, TimeUnit.SECONDS);
        CypherStatement.Simple statement2 = statement1.addParameter("b", 2);
        CypherStatement.Simple statement3 = statement2.withParameters(singletonMap("a", 1));
        CypherStatement.Simple statement4 = statement3.addParameter("c", 3);

        assertThat(statement0.query()).isEqualTo(QUERY);
        assertThat(statement0.parameters()).containsExactly(entry("d", 4));
        assertThat(statement0.timeout()).isEmpty();

        assertThat(statement1.query()).isEqualTo(QUERY);
        assertThat(statement1.parameters()).containsExactly(entry("d", 4));
        assertThat(statement1.timeout()).hasValue(10000L);

        assertThat(statement2.query()).isEqualTo(QUERY);
        assertThat(statement2.parameters()).containsExactly(entry("d", 4), entry("b", 2));
        assertThat(statement2.timeout()).hasValue(10000L);

        assertThat(statement3.query()).isEqualTo(QUERY);
        assertThat(statement3.parameters()).containsExactly(entry("a", 1));
        assertThat(statement3.timeout()).hasValue(10000L);

        assertThat(statement4.query()).isEqualTo(QUERY);
        assertThat(statement4.parameters()).containsExactly(entry("a", 1), entry("c", 3));
        assertThat(statement4.timeout()).hasValue(10000L);
    }

    @Test
    public void submittableStatement() {
        CypherGremlinClient client = CypherGremlinClient.inMemory(null);

        CypherStatement.Submittable statement = client.statement(QUERY)
            .withParameters(singletonMap("a", 1))
            .addParameter("b", 2)
            .addParameter("c", 3)
            .withTimeout(10, TimeUnit.SECONDS);

        assertThat(statement.query()).isEqualTo(QUERY);
        assertThat(statement.parameters()).containsExactly(entry("a", 1), entry("b", 2), entry("c", 3));
        assertThat(statement.timeout()).hasValue(10000L);
        assertThat(statement.requestOptions().getTimeout()).hasValue(10000L);

        assertThat(statement).isInstanceOf(CypherStatement.Submittable.class);
    }
}
