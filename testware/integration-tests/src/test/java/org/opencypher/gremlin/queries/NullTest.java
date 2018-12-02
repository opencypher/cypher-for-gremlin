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
package org.opencypher.gremlin.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class NullTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Before
    public void setUp() {
        submitAndGet(DELETE_ALL);
    }

    @Test
    public void ignoreNullWhenSettingProperty() {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH (a:DoesNotExist) " +
                "SET a.prop = 42 " +
                "RETURN a"
        );

        assertThat(results)
            .extracting("a")
            .containsExactly((Object) null);
    }

    @Test
    public void ignoreNullWhenRemovingProperty() {
        List<Map<String, Object>> results = submitAndGet(
            "OPTIONAL MATCH (a:DoesNotExist) " +
                "REMOVE a.prop " +
                "RETURN a"
        );

        assertThat(results)
            .extracting("a")
            .containsExactly((Object) null);
    }
}
