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

import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.junit.Ignore;
import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

import java.util.HashMap;

import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

public class MatchTest {

    @Test
    public void matchNode() {
        assertThat(parse(
            "MATCH (p:person) RETURN p.name AS name"
        ))
            .hasTraversalBeforeReturn(
                __.V().as("p")
                    .where(__.select("p").hasLabel("person"))
                    .select("p")
            );
    }

    @Test
    public void matchAllNodes() {
        assertThat(parse(
            "MATCH (n) RETURN n"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .select("n")
            );
    }

    @Test
    public void reuseAliasInSimplePattern() throws Exception {
        assertThat(parse(
            "MATCH (a)-[r]->(a) RETURN a"
        )).hasTraversalBeforeReturn(
            __.V().as("a")
                .outE().as("r").inV()
                .as("  GENERATED1")
                .where(__.select("  GENERATED1").where(P.eq("a")))
                .select("a")
        );
    }

    @Test
    public void reuseAliasInChainedPattern() throws Exception {
        assertThat(parse(
            "MATCH (a)-[r1]->(b)-[r2]->(b) RETURN b"
        )).hasTraversalBeforeReturn(
            __.V().as("a")
                .outE().as("r1").inV()
                .as("b")
                .outE().as("r2").inV()
                .as("  GENERATED1")
                .where(__.select("  GENERATED1").where(P.eq("b")))
                .where(__.select("r1").where(P.neq("r2")))
                .select("b")
        );
    }

    @Test
    public void reuseAliasInTwoPatterns() throws Exception {
        assertThat(parse(
            "MATCH (a)-[r1]->(b), (b)-[r2]->(b) RETURN b"
        )).hasTraversalBeforeReturn(
            __.V().as("a")
                .outE().as("r1").inV()
                .as("b")
                .V().as("  GENERATED1")
                .where(__.select("  GENERATED1").where(P.eq("b")))
                .outE().as("r2").inV()
                .as("  GENERATED2")
                .where(__.select("  GENERATED2").where(P.eq("b")))
                .where(__.select("r1").where(P.neq("r2")))
                .select("b")
        );
    }

    @Test
    public void comparison() {
        assertThat(parse(
            "MATCH (p:person) WHERE 29 <= p.age < 32 RETURN p"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("p")
                    .where(
                        __.and(
                            __.select("p").hasLabel("person"),
                            __.select("p").values("age").is(P.gte(29)),
                            __.select("p").values("age").is(P.lt(32))
                        )
                    )
                    .select("p")
            );
    }

    @Test
    public void countStar() {
        assertThat(parse(
            "MATCH (n) RETURN count(*)"
        ))
            .hasTraversalBeforeReturn(
                __.V().as("n")
            );
    }

    @Test
    public void countNodes() {
        assertThat(parse(
            "MATCH (p:person) RETURN count(p)"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("p")
                    .where(__.select("p").hasLabel("person"))
                    .select("p")
            );
    }

    @Test
    public void collect() {
        assertThat(parse(
            "MATCH (p:person) RETURN collect(p.name)"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("p")
                    .where(__.select("p").hasLabel("person"))
                    .select("p")
            );
    }

    @Test
    public void matchNodeWhere() {
        assertThat(parse(
            "MATCH (n:person)-[r:knows]->(friend:person)\n" +
                "WHERE n.name = \"marko\"\n" +
                "RETURN friend"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .outE("knows").as("r").inV()
                    .as("friend")
                    .where(
                        __.and(
                            __.select("n").hasLabel("person"),
                            __.select("friend").hasLabel("person"),
                            __.select("n").values("name").is(P.eq("marko"))
                        )
                    )
                    .select("friend")
            );
    }

    @Test
    public void matchRelationWhere() {
        assertThat(parse(
            "MATCH (n)-[r {weight: 1.0}]-(m) " +
                "RETURN n, m"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .bothE().as("r").otherV()
                    .as("m")
                    .where(
                        __.select("r").values("weight").is(P.eq(1.0))
                    )
                    .select("n", "m")
            );
    }

    @Test
    public void matchRelationTypeWhere() {
        assertThat(parse(
            "MATCH (n)-[r]-(m)\n" +
                "WHERE type(r) = \"knows\"\n" +
                "RETURN n, m"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .bothE().as("r").otherV()
                    .as("m")
                    .where(
                        __.select("r").label().is(P.eq("knows"))
                    )
                    .select("n", "m")
            );
    }

    @Test
    public void matchMultiplePaths() {
        assertThat(parse(
            "MATCH (n:person {name: \"marko\"})\n" +
                "MATCH (n)-[r1:created]->(lop:software {name: \"lop\"})" +
                "    <-[r2:created]-(colleague:person)\n" +
                "RETURN colleague"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n")
                    .where(
                        __.and(
                            __.select("n").values("name").is(P.eq("marko")),
                            __.select("n").hasLabel("person")
                        )
                    )
                    .V().as("  GENERATED1")
                    .where(__.select("  GENERATED1").where(P.eq("n")))
                    .outE("created").as("r1").inV()
                    .as("lop")
                    .inE("created").as("r2").outV()
                    .as("colleague")
                    .where(
                        __.and(
                            __.select("lop").values("name").is(P.eq("lop")),
                            __.select("lop").hasLabel("software"),
                            __.select("colleague").hasLabel("person"),
                            __.select("r1").where(P.neq("r2"))
                        )
                    )
                    .select("colleague")
            );
    }

    @Test
    public void allRelations() {
        assertThat(parse(
            "MATCH (n1)-[r]->(n2) RETURN DISTINCT type(r)"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n1").outE().as("r").inV().as("n2")
                    .select("r")
            );
    }

    @Test
    public void pathAssignment() {
        assertThat(parse(
            "MATCH p = (n1)<-[r1]-(n2)-[r2]->(n3) RETURN p"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n1").inE().as("r1").outV().as("n2").outE().as("r2").inV().as("n3")
                    .path().as("p")
                    .where(__.select("r1").where(P.neq("r2")))
                    .select("p")
            );
    }

    @Test
    @Ignore("WhereWalker should consider the path 'calculus'")
    public void pathLengthWhere() throws Exception {
        assertThat(parse(
            "MATCH p = (n)-[r]->(m) " +
                "WHERE length(p) = 1 " +
                "RETURN n, m"
        ))
            .hasTraversalBeforeReturn(
                __.V().as("n")
                    .outE().as("r").inV().as("m")
                    .path().as("p")
                    .where(__.select("p").count(Scope.local).is(P.isEq(3)))
                    .select("n", "m")
            );
    }

    @Test
    public void matchMultiple() {
        assertThat(parse(
            "MATCH (n1)-[r]->(n2) RETURN n1.name, type(r), n2.name"
        ))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("n1").outE().as("r").inV().as("n2")
                    .select("n1", "r", "n2")
            );
    }

    @Test
    public void matchMultipleComplex() throws Exception {
        assertThat(parse(
            "MATCH (j:person)-[c1:created]->(lop:software {name: \"ripple\"})\n" +
                "MATCH (j)<-[k:knows]-(m:person)-[c2:created]->(s:software)\n" +
                "RETURN s, m"))
            .hasTraversalBeforeReturn(
                __.V()
                    .as("j")
                    .outE("created").as("c1")
                    .inV().as("lop")
                    .where(
                        __.and(
                            __.select("j").hasLabel("person"),
                            __.select("lop").values("name").is(P.eq("ripple")),
                            __.select("lop").hasLabel("software")
                        )
                    )

                    .V().as("  GENERATED1")
                    .where(__.select("  GENERATED1").where(P.eq("j")))
                    .inE("knows").as("k")
                    .outV().as("m")
                    .outE("created").as("c2").inV().as("s")
                    .where(
                        __.and(
                            __.select("m").hasLabel("person"),
                            __.select("s").hasLabel("software")
                        )
                    )

                    .select("s", "m")
            );
    }

    @Test
    public void multipleMatchRelated() {
        assertThat(parse(
            "MATCH (p:person) " +
                "MATCH (p)<-[k:knows]-(m:person)" +
                "RETURN p.name AS name"
        ))
            .hasTraversalBeforeReturn(
                __.V().as("p")
                    .where(__.select("p").hasLabel("person"))
                    .V().as("  GENERATED1")
                    .where(__.select("  GENERATED1").where(P.eq("p")))
                    .inE("knows").as("k").outV().as("m")
                    .where(__.select("m").hasLabel("person"))
                    .select("p")
            );
    }

    @Test
    public void multipleMatchUnrelated() {
        assertThat(parse(
            "MATCH (p:person) " +
                "MATCH (o)<-[k:knows]-(m:person)" +
                "RETURN p.name AS name"
        ))
            .hasTraversalBeforeReturn(
                __.V().as("p")
                    .where(__.select("p").hasLabel("person"))
                    .V().as("o").inE("knows").as("k").outV().as("m")
                    .where(__.select("m").hasLabel("person"))
                    .select("p")
            );
    }

    @Test
    public void multipleMatchRelatedDivided() {
        assertThat(parse(
            "MATCH (p:person) " +
                "MATCH (s:software) " +
                "MATCH (p)<-[k:knows]-(m:person)" +
                "RETURN p.name AS name"
        ))
            .hasTraversalBeforeReturn(
                __
                    .V().as("p").where(__.select("p").hasLabel("person"))
                    .V().as("s").where(__.select("s").hasLabel("software"))
                    .V().as("  GENERATED1").where(__.select("  GENERATED1").where(P.eq("p")))
                    .inE("knows").as("k").outV()
                    .as("m").where(__.select("m").hasLabel("person"))
                    .select("p")
            );
    }

    @Test
    public void literalsAfterParams() {
        final int param1 = 11;
        final int param2 = 22;

        HashMap<String, Object> params = new HashMap<>();
        params.put("1", param1);
        params.put("2", param2);

        assertThat(parse(
            " MATCH (advertiser)-[ahp:ADV_HAS_PRODUCT]->(out)-[ahv:AP_HAS_VALUE]->(red)<-[aav:AA_HAS_VALUE]-(a)\n" +
                "WHERE advertiser.id = $1\n" +
                "AND a.id = $2\n" +
                "AND red.name = 'red'\n" +
                "AND out.name = 'product1'\n" +
                "RETURN out.name",
            params
        )).hasTraversalBeforeReturn(__.V()
            .as("advertiser")
            .outE("ADV_HAS_PRODUCT").as("ahp")
            .inV().as("out")
            .outE("AP_HAS_VALUE").as("ahv")
            .inV().as("red")
            .inE("AA_HAS_VALUE").as("aav")
            .outV().as("a")
            .where(
                __.and(
                    __.select("advertiser").values("id").is(P.eq(param1)),
                    __.select("a").values("id").is(P.eq(param2)),
                    __.select("red").values("name").is(P.eq("red")),
                    __.select("out").values("name").is(P.eq("product1"))
                )
            )
            .select("out")
        );
    }

    @Test
    public void booleanOperators() {
        assertThat(parse(
            "MATCH (n)\n" +
                "WHERE n.p = 4 OR (n.p > 6 AND NOT n.p < 10)\n" +
                "RETURN n"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .where(
                    __.or(
                        __.select("n").values("p").is(P.eq(4)),
                        __.and(
                            __.select("n").values("p").is(P.gt(6)),
                            __.select("n").values("p").is(P.gte(10))
                        )
                    )
                )
                .select("n")
        );
    }

    @Test
    public void booleanLiteral() throws Exception {
        assertThat(parse(
            "MATCH (n)\n" +
                "WHERE NOT(false)\n" +
                "RETURN n"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .where(__.not(__.constant(false).is(P.eq(true))))
                .select("n")
        );
    }

    @Test
    public void labelPredicate() throws Exception {
        assertThat(parse(
            "MATCH (n)\n" +
                "RETURN (n:Foo)"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .select("n")
        );
    }

    @Test
    public void matchNodeSeparatelyFromPattern() throws Exception {
        assertThat(parse(
            "MATCH (a {name: 'A'}), (c {name: 'C'})\n" +
                "MATCH (a)-[r]->(b)\n" +
                "RETURN a, b, c"
        )).hasTraversalBeforeReturn(
            __
                .V().as("a")
                .V().as("c")
                .where(
                    __.and(
                        __.select("a").values("name").is(P.eq("A")),
                        __.select("c").values("name").is(P.eq("C"))
                    )
                )
                .V().as("  GENERATED1")
                .where(
                    __.select("  GENERATED1").where(P.eq("a"))
                )
                .outE().as("r").inV()
                .as("b")
                .select("a", "b", "c")
        );
    }

    @Test
    public void patternInReturn() throws Exception {
        assertThat(parse(
            "MATCH (n) " +
                "WHERE (n)-[:created]->(:software) " +
                "RETURN n"
        )).hasTraversalBeforeReturn(
            __.V()
                .as("n")
                .where(
                    __.select("n").outE("created").inV().hasLabel("software")
                )
                .select("n")
        );
    }
}
