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

import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstHelpers.__;

public class WithTest {

    @Test
    public void singleName() throws Exception {
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
    public void aliasShadow() throws Exception {
        assertThat(parse(
            "MATCH (n) WITH n.name AS n RETURN n"
        )).hasTraversalBeforeReturn(
            __.V().as("n")
                .select("n").values("name").as("n")
                .select("n")
        );
    }

    @Test
    public void where() throws Exception {
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
        ).hasTraversalBeforeReturn(
            __.V().as("a")
                .where(__.select("a").hasLabel("artist"))
                .where(__.select("a").values("age").is(P.gt(18)))
                .select("a")
        );
    }

    @Test
    public void whereOrderBy() throws Exception {
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
        ).hasTraversalBeforeReturn(
            __.V().as("a")
                .where(__.select("a").hasLabel("artist"))
                .where(__.select("a").values("age").is(P.gt(18)))
                .select("a").values("name").as("  FRESHID50")
                .select("  FRESHID50")
                .order().by(__.select("  FRESHID50"), Order.incr)
                .select("  FRESHID50")
        );
    }

    @Test
    public void orderBySingleName() throws Exception {
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
        ).hasTraversalBeforeReturn(
            __.V().as("a")
                .where(__.select("a").hasLabel("artist"))
                .select("a").values("name").as("  FRESHID26")
                .select("  FRESHID26")
                .order().by(__.select("  FRESHID26"), Order.incr)
                .select("  FRESHID26")
        );
    }

    @Test
    public void orderByMultipleNames() throws Exception {
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
        ).hasTraversalBeforeReturn(
            __.V().as("a")
                .inE("writtenBy").as("  UNNAMED17").outV().as("s")
                .where(
                    __.and(
                        __.select("a").hasLabel("artist"),
                        __.select("s").hasLabel("song")
                    )
                )
                .select("a").values("name").as("  FRESHID49")
                .select("s").values("name").as("  FRESHID57")
                .select("  FRESHID49", "  FRESHID57")
                .order()
                .by(__.select("  FRESHID49"), Order.incr)
                .by(__.select("  FRESHID57"), Order.incr)
                .select("  FRESHID49", "  FRESHID57")
        );
    }

    @Test
    public void orderBySkipLimit() throws Exception {
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
        ).hasTraversalBeforeReturn(
            __.V().as("a")
                .where(__.select("a").hasLabel("artist"))
                .select("a").values("name").as("  FRESHID26")
                .select("  FRESHID26")
                .order().by(__.select("  FRESHID26"), Order.incr)
                .skip(1)
                .limit(2)
                .select("  FRESHID26")
        );
    }
}
