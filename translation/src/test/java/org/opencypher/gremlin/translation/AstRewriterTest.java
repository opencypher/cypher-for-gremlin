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
package org.opencypher.gremlin.translation;

import static org.opencypher.gremlin.translation.CypherAst.parse;
import static org.opencypher.gremlin.translation.ir.helpers.CypherAstAssertions.assertThat;

import org.junit.Test;

public class AstRewriterTest {

    @Test
    public void implicitWhere() throws Exception {
        assertThat(parse(
            "MATCH (n:Label {prop: 'value'})-[r]->(m) RETURN n"
        )).normalizedTo(
            "MATCH (n)-[r]->(m) WHERE n.prop = 'value' AND (n:Label) RETURN n"
        );
    }

    @Test
    public void implicitWhereIsNotApplied() throws Exception {
        assertThat(parse(
            "MATCH (n:Label {prop: 'value'}) RETURN n"
        )).normalizedTo(
            "MATCH (n:Label {prop: 'value'}) RETURN n"
        );
    }

    @Test
    public void whereClauseNormalizer() throws Exception {
        assertThat(parse(
            "MATCH (n) WHERE n.prop = 'value' AND (n:Label) RETURN n"
        )).normalizedTo(
            "MATCH (n:Label {prop: 'value'}) RETURN n"
        );
    }

    @Test
    public void matchFalseRewrite() throws Exception {
        assertThat(parse(
            "MATCH (n) WHERE 1 = 0 AND n.name = \"marko\" RETURN n"
        )).normalizedTo(
            "MATCH (n) WHERE 1 = 0 RETURN n"
        );
    }

    @Test
    public void matchTrueRewrite() throws Exception {
        assertThat(parse(
            "MATCH (n) WHERE 1 = 1 OR n.name = \"marko\" RETURN n"
        )).normalizedTo(
            "MATCH (n) WHERE 1 = 1 RETURN n"
        );
    }

    @Test
    public void matchSingleName() throws Exception {
        assertThat(parse(
            "MATCH (a:artist) " +
                "RETURN a.name"
        )).normalizedTo(
            "MATCH (a:artist) " +
                "RETURN a.name AS `a.name`"
        );
    }

    @Test
    public void multipleNames() throws Exception {
        assertThat(parse(
            "MATCH (a:artist)<-[:writtenBy]-(s:song) " +
                "RETURN a.name, s.name"
        )).normalizedTo(
            "MATCH (a:artist)<-[:writtenBy]-(s:song) " +
                "RETURN a.name AS `a.name`, s.name AS `s.name`"
        );
    }

    @Test
    public void matchWhere() throws Exception {
        assertThat(parse(
            "MATCH (a:artist) " +
                "WITH a WHERE a.age > 18 " +
                "RETURN a.name"
        )).normalizedTo(
            "MATCH (a:artist) " +
                "WITH a AS a " +
                "WITH a AS a, a.age > 18 AS `  FRESHID36`" +
                "WITH a AS a, `  FRESHID36`AS `  FRESHID36`" +
                "WHERE `  FRESHID36`" +
                "WITH a AS a " +
                "RETURN a.name AS `a.name`"
        );
    }

    @Test
    public void matchWhereOrderBy() throws Exception {
        assertThat(parse(
            "MATCH (a:artist) " +
                "WITH a WHERE a.age > 18 " +
                "RETURN a.name " +
                "ORDER BY a.name"
        )).normalizedTo(
            "MATCH (a:artist) " +
                "WITH a AS a " +
                "WITH a AS a, a.age > 18 AS `  FRESHID36`" +
                "WITH a AS a, `  FRESHID36`AS `  FRESHID36`" +
                "WHERE `  FRESHID36`" +
                "WITH a AS a " +
                "WITH a.name AS `  FRESHID50` " +
                "WITH `  FRESHID50` AS `  FRESHID50` " +
                "ORDER BY `  FRESHID50` " +
                "RETURN `  FRESHID50` AS `a.name`"
        );
    }

    @Test
    public void matchOrderBySingleName() throws Exception {
        assertThat(parse(
            "MATCH (a:artist) " +
                "RETURN a.name " +
                "ORDER BY a.name"
        )).normalizedTo(
            "MATCH (a:artist) " +
                "WITH a.name AS `  FRESHID26` " +
                "WITH `  FRESHID26` AS `  FRESHID26` " +
                "ORDER BY `  FRESHID26` " +
                "RETURN `  FRESHID26` AS `a.name`"
        );
    }

    @Test
    public void matchOrderByMultipleNames() throws Exception {
        assertThat(parse(
            "MATCH (a:artist)<-[:writtenBy]-(s:song) " +
                "RETURN a.name, s.name " +
                "ORDER BY a.name, s.name"
        )).normalizedTo(
            "MATCH (a:artist)<-[:writtenBy]-(s:song) " +
                "WITH a.name AS `  FRESHID49`, s.name AS `  FRESHID57` " +
                "WITH `  FRESHID49` AS `  FRESHID49`, `  FRESHID57` AS `  FRESHID57` " +
                "ORDER BY `  FRESHID49`, `  FRESHID57` " +
                "RETURN `  FRESHID49` AS `a.name`, `  FRESHID57` AS `s.name`"
        );
    }

    @Test
    public void matchOrderBySkipLimit() throws Exception {
        assertThat(parse(
            "MATCH (a:artist) " +
                "RETURN a.name " +
                "ORDER BY a.name " +
                "SKIP 1 LIMIT 2"
        )).normalizedTo(
            "MATCH (a:artist) " +
                "WITH a.name AS `  FRESHID26` " +
                "WITH `  FRESHID26` AS `  FRESHID26` " +
                "ORDER BY `  FRESHID26` SKIP 1 LIMIT 2 " +
                "RETURN `  FRESHID26` AS `a.name`"
        );
    }

}
