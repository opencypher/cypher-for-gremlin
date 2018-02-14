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
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ComparisonTest {

    private static final String MATCH_PERSON = "MATCH (p:person) WHERE %s RETURN p.name AS name";
    private static final String[] EVERYONE = new String[]{"marko", "vadas", "josh", "peter"};

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    @Test
    public void numericEquality() throws Exception {
        assertComparison(MATCH_PERSON, "p.age = 29", "marko");
        assertComparison(MATCH_PERSON, "p.age = 29.0", "marko");
        assertComparison(MATCH_PERSON, "p.age <> 29", "vadas", "josh", "peter");
        assertComparison(MATCH_PERSON, "p.age <> 29.0", "vadas", "josh", "peter");
    }

    @Test
    public void numericComparison() throws Exception {
        assertComparison(MATCH_PERSON, "p.age <  29", "vadas");
        assertComparison(MATCH_PERSON, "p.age <= 29", "vadas", "marko");
        assertComparison(MATCH_PERSON, "p.age >  32", "peter");
        assertComparison(MATCH_PERSON, "p.age >= 32", "josh", "peter");
    }

    @Test
    public void numericMultiComparison() throws Exception {
        assertComparison(MATCH_PERSON, "29 <  p.age <  32");
        assertComparison(MATCH_PERSON, "29 <  p.age <= 32", "josh");
        assertComparison(MATCH_PERSON, "29 <= p.age <  32", "marko");
        assertComparison(MATCH_PERSON, "29 <= p.age <= 32", "marko", "josh");
    }

    @Test
    public void symbolicEquality() throws Exception {
        assertComparison(MATCH_PERSON, "p.name = \"marko\"", "marko");
        assertComparison(MATCH_PERSON, "p.name <> \"marko\"", "vadas", "josh", "peter");
    }

    @Test
    public void symbolicComparison() throws Exception {
        assertComparison(MATCH_PERSON, "p.name <  \"marko\"", "josh");
        assertComparison(MATCH_PERSON, "p.name <= \"marko\"", "josh", "marko");
        assertComparison(MATCH_PERSON, "p.name >  \"marko\"", "peter", "vadas");
        assertComparison(MATCH_PERSON, "p.name >= \"marko\"", "marko", "peter", "vadas");
    }

    @Test
    public void symbolicMultiComparison() throws Exception {
        assertComparison(MATCH_PERSON, "\"marko\" <  p.name <  \"peter\"");
        assertComparison(MATCH_PERSON, "\"marko\" <  p.name <= \"peter\"", "peter");
        assertComparison(MATCH_PERSON, "\"marko\" <= p.name <  \"peter\"", "marko");
        assertComparison(MATCH_PERSON, "\"marko\" <= p.name <= \"peter\"", "marko", "peter");
    }

    @Test
    public void nullability() throws Exception {
        assertComparison(MATCH_PERSON, "p.name IS NULL");
        assertComparison(MATCH_PERSON, "p.name IS NULL AND p.age IS NULL");
        assertComparison(MATCH_PERSON, "p.foo  IS NULL", EVERYONE);
        assertComparison(MATCH_PERSON, "p.foo  IS NULL AND p.bar IS NULL", EVERYONE);
    }

    @Test
    public void nonNullability() throws Exception {
        assertComparison(MATCH_PERSON, "p.foo  IS NOT NULL");
        assertComparison(MATCH_PERSON, "p.name IS NOT NULL", EVERYONE);
    }

    private void assertComparison(String query, String condition, String... names) throws Exception {
        List<Map<String, Object>> results = submitAndGet(format(query, condition));
        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder((Object[]) names);
    }
}
