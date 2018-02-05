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
package org.opencypher.gremlin;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.opencypher.gremlin.rules.TinkerGraphServerEmbedded;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class SetTest {

    @ClassRule
    public static final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher);
    }

    @Test
    public void setAndGetString() throws Exception {
        assertThat(setAndGetProperty("1")).containsExactly(1L);
    }

    @Test
    public void setAndGetQuotedString() throws Exception {
        assertThat(setAndGetProperty("'123'")).containsExactly("123");
    }

    @Test
    public void setAndGetEmptyList() throws Exception {
        assertThat(setAndGetProperty("[]")).containsExactly((Object) null);
    }

    @Test
    @Ignore("https://trello.com/c/b9vaFznT/223")
    public void setAndGetList() throws Exception {
        assertThat(setAndGetProperty("[1, 2, 3]")).containsExactly(asList(1L, 2L, 3L));
    }

    @Test
    public void setAndGetMap() throws Exception {
        assertThat(setAndGetProperty("{key: 'value'}")).containsExactly(singletonMap("key", "value"));
    }

    private List<Object> setAndGetProperty(String value) throws Exception {
        String query = "MATCH (n) SET n.property = %s RETURN n.property AS prop LIMIT 1";
        return submitAndGet(format(query, value)).stream().map(r -> r.get("prop")).collect(toList());
    }

}
