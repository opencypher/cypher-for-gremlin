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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;
import static org.opencypher.gremlin.translation.groovy.StringTranslationUtils.toLiteral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithNeptune;
import org.opencypher.gremlin.groups.UsesCollectionsInProperties;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.groovy.StringTranslationUtils;

public class LiteralTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private Map<String, Object> literalMap;

    @Before
    public void setUp() {
        submitAndGet(DELETE_ALL);

        literalMap = new LinkedHashMap<>();
        literalMap.put("p0", 13L);
        literalMap.put("p1", -40000L);
        literalMap.put("p2", 3.14);
        literalMap.put("p3", 6.022e23);
        literalMap.put("p4", "Hello");
        literalMap.put("p5", true);
        literalMap.put("p6", false);
        literalMap.put("p7", null);
        literalMap.put("p8", asList(13L, -40000L));
        literalMap.put("p9", asList("Hello", "World"));
        literalMap.put("p10", asList(true, false));
        literalMap.put("p11", new ArrayList<>());
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    @Category(UsesCollectionsInProperties.ListDataType.class)
    public void create() {
        String fieldMap = literalMap.entrySet().stream()
            .map(e -> e.getKey() + ":" + toLiteral(e.getValue()))
            .collect(joining(","));
        List<Map<String, Object>> create = submitAndGet(
            "CREATE(n:L {" + fieldMap + "}) RETURN n"
        );
        List<Map<String, Object>> match = submitAndGet("MATCH (n:L) RETURN n");

        assertThat(create)
            .extracting("n")
            .extracting(byElementProperty(literalMap.keySet().toArray(new String[]{})))
            .containsExactly(tuple(literalMap.values().toArray()));

        assertThat(match)
            .extracting("n")
            .extracting(byElementProperty(literalMap.keySet().toArray(new String[]{})))
            .containsExactly(tuple(literalMap.values().toArray()));
    }

    @Test
    public void with() {
        String literals = literalMap.values().stream()
            .map(StringTranslationUtils::toLiteral)
            .collect(joining(","));
        List<Map<String, Object>> results = submitAndGet(
            "WITH [" + literals + "] AS list " +
                "RETURN list"
        );

        assertThat(results)
            .extracting("list")
            .containsExactly(new ArrayList<>(literalMap.values()));
    }

    @Test
    public void unwind() {
        String literals = literalMap.values().stream()
            .map(StringTranslationUtils::toLiteral)
            .collect(joining(","));
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [" + literals + "] AS list " +
                "RETURN list"
        );

        assertThat(results)
            .extracting("list")
            .containsExactlyElementsOf(literalMap.values());
    }

    @Test
    public void returnList() {
        String literals = literalMap.values().stream()
            .map(StringTranslationUtils::toLiteral)
            .collect(joining(","));
        List<Map<String, Object>> results = submitAndGet(
            "RETURN [" + literals + "] AS list"
        );

        assertThat(results)
            .extracting("list")
            .containsExactly(new ArrayList<>(literalMap.values()));
    }

    @Test
    public void returnAs() {
        String returnItems = literalMap.entrySet().stream()
            .map(e -> toLiteral(e.getValue()) + " AS " + e.getKey())
            .collect(joining(","));
        List<Map<String, Object>> results = submitAndGet(
            "RETURN " + returnItems
        );

        assertThat(results)
            .containsExactly(literalMap);
    }

    @Test
    @Category(SkipWithNeptune.EmptyMap.class)
    public void returnEmptyMap() {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN {} AS map"
        );

        assertThat(results)
            .extracting("map")
            .containsExactly(new HashMap<>());
    }

    @Test
    public void returnMap() {
        List<Map<String, Object>> results = submitAndGet(
            "RETURN {k1: 12, k2: 'Hello', k3: true} AS map"
        );

        Map<String, Object> map = new HashMap<>();
        map.put("k1", 12L);
        map.put("k2", "Hello");
        map.put("k3", true);

        assertThat(results)
            .extracting("map")
            .containsExactly(map);
    }
}
