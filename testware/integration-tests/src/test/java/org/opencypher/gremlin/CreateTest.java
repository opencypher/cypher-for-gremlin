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

import org.apache.tinkerpop.gremlin.driver.exception.ResponseException;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.TinkerGraphServerEmbedded;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class CreateTest {

    @ClassRule
    public static final TinkerGraphServerEmbedded gremlinServer = new TinkerGraphServerEmbedded();

    @Before
    public void setUp() {
        gremlinServer.client().submitGremlin("g.V().drop()");
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.client().submitCypher(cypher);
    }

    @Test
    public void matchCreateLoop() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (root:R) " +
                "CREATE (root)-[link:LINK]->(root) " +
                "RETURN root, link"
        );

        assertThat(results).hasSize(1);
        Map<String, Object> result = results.get(0);
        Vertex root = (Vertex) result.get("root");
        Edge link = (Edge) result.get("link");
        assertThat(link.inVertex().id()).isEqualTo(root.id());
        assertThat(link.outVertex().id()).isEqualTo(root.id());
    }

    @Test
    public void createMatchMix() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (marko:person {name: \"Marko\"})-[r:knows]->(vadas:person {name: \"Vadas\"}) " +
                "WITH marko AS m " +
                "MATCH (m)-[r:knows]->(friend) " +
                "RETURN friend.name "
        );

        assertThat(results)
            .extracting("friend.name")
            .containsExactly("Vadas");
    }

    @Test
    public void createMatchMix2() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (marko:person {name: \"Marko\"}) " +
                "CREATE (josh:person {name: \"Josh\"}) " +
                "CREATE (lop:software {name: \"lop\"}) " +
                "CREATE (marko)-[:created]->(lop) " +
                "CREATE (josh)-[:created]->(lop) " +
                "WITH marko AS m " +
                "MATCH (m)-[:created]->()<-[:created]-(colleague) " +
                "RETURN colleague.name"
        );

        assertThat(results)
            .extracting("colleague.name")
            .containsExactly("Josh");
    }

    @Test
    public void matchCreateMix() throws Exception {
        long rootId = (long) ((Vertex) submitAndGet("CREATE (d:D) RETURN d").get(0).get("d")).id();

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (d:D) " +
                "CREATE (e1:E {name:'0'}), (e2:E {name:'1'}) " +
                "CREATE (d)-[:MCREATE]->(e1), (d)-[:MCREATE]->(e2)"
        );

        assertThat(results).isEmpty();

        List<Long> createdIds = submitAndGet("MATCH (n:E) RETURN n")
            .stream()
            .map(m -> (Vertex) m.get("n"))
            .map(v -> (Long) v.id())
            .collect(toList());

        assertThat(createdIds).hasSize(2);

        List<Edge> matchCreates = submitAndGet("MATCH ()-[r:MCREATE]->() RETURN r")
            .stream()
            .map(m -> (Edge) m.get("r"))
            .collect(toList());
        assertThat(matchCreates).hasSize(2);

        Edge edge1 = matchCreates.get(0);
        assertThat(edge1.outVertex().id()).isEqualTo(rootId);
        assertThat(createdIds).contains((Long) edge1.inVertex().id());

        Edge edge2 = matchCreates.get(1);
        assertThat(edge2.outVertex().id()).isEqualTo(rootId);
        assertThat(createdIds).contains((Long) edge2.inVertex().id());
    }

    @Test
    public void matchDeleteCreate() throws Exception {
        List<Map<String, Object>> toBeDeleted = submitAndGet("CREATE (a:A)-[r:TBDELETED]->(b:B) RETURN COUNT(r)");

        assertThat(toBeDeleted)
            .extracting("COUNT(r)")
            .containsExactly(1L);

        submitAndGet("MATCH (a:A)-[r]->(b)\n" +
            //"DELETE r\n" + TODO fix GraphTraversal#drop traversal yields no outgoing objects.
            "CREATE (b)-[:RECREATED]->(a) RETURN b");

        toBeDeleted = submitAndGet("MATCH ()-[r:TBDELETED]->() RETURN COUNT(r)");

        assertThat(toBeDeleted)
            .extracting("COUNT(r)")
            .containsExactly(1L);

        List<Map<String, Object>> recreated = submitAndGet("MATCH ()-[r:RECREATED]->() RETURN COUNT(r)");

        assertThat(recreated)
            .extracting("COUNT(r)")
            .containsExactly(1L);
    }

    @Test
    public void matchCreateEdgeUnrelatedNodes() throws Exception {
        Map<String, Object> created = submitAndGet(
            "CREATE (marko:person {name: \"marko\"}), " +
                "(vadas:person {name: \"vadas\"}) " +
                "RETURN marko, vadas"
        ).get(0);
        long markoId = (long) ((Vertex) created.get("marko")).id();
        long vadasId = (long) ((Vertex) created.get("vadas")).id();

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (marko:person),(vadas:person) " +
                "WHERE marko.name = 'marko' AND vadas.name = 'vadas' " +
                "CREATE (marko)-[r:matchCreates]->(vadas)"
        );

        assertThat(results).isEmpty();

        Edge edge = (Edge) submitAndGet("MATCH ()-[r:matchCreates]->() RETURN r").get(0).get("r");

        assertThat(edge.outVertex().id()).isEqualTo(markoId);
        assertThat(edge.inVertex().id()).isEqualTo(vadasId);
    }

    @Test
    public void createComplex1() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (marko:person {name: \"marko\", age: 29}), " +
                "(vadas:person {name: \"vadas\", age: 27}), " +
                "(marko)-[r:knows {weight: 0.5}]->(vadas)"
        );
        List<Map<String, Object>> vertices = submitAndGet("MATCH (n) RETURN n");
        List<Map<String, Object>> edges = submitAndGet("MATCH ()-[r]->() RETURN r");

        assertThat(results).isEmpty();
        assertThat(vertices).hasSize(2);
        assertThat(edges).hasSize(1);
    }

    @Test
    public void createComplex2() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (n1:ln1)<-[r1:lr1]-(n2:ln2)-[r2:lr2]->(n3:ln3)"
        );

        long vertices = (long) submitAndGet("MATCH (n) RETURN COUNT(n) AS vertices").get(0).get("vertices");
        long edges = (long) submitAndGet("MATCH ()-[r]->() RETURN COUNT(r) AS edges").get(0).get("edges");

        assertThat(results).isEmpty();
        assertThat(vertices).isEqualTo(3L);
        assertThat(edges).isEqualTo(2L);
    }

    @Test
    public void createForAll() throws Exception {
        List<Map<String, Object>> beforeCreate = submitAndGet(
            "MATCH (n) RETURN COUNT(n) AS ns"
        );

        assertThat(beforeCreate)
            .extracting("ns")
            .containsExactly(0L);

        submitAndGet("CREATE ()");
        List<Map<String, Object>> afterCreate = submitAndGet(
            "MATCH (n) RETURN COUNT(n) AS ns"
        );

        assertThat(afterCreate)
            .extracting("ns")
            .containsExactly(1L);

        submitAndGet("MATCH () CREATE ()");
        List<Map<String, Object>> afterDuplicate = submitAndGet(
            "MATCH (n) RETURN COUNT(n) AS ns"
        );

        assertThat(afterDuplicate)
            .extracting("ns")
            .containsExactly(2L);

        submitAndGet("MATCH () CREATE ()");
        List<Map<String, Object>> afterDuplicateTwice = submitAndGet(
            "MATCH (n) RETURN COUNT(n) AS ns"
        );

        assertThat(afterDuplicateTwice)
            .extracting("ns")
            .containsExactly(4L);
    }

    @Test
    public void createNullProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (n {foo: \"bar\", property: null}) RETURN n.foo AS f"
        );

        assertThat(results)
            .extracting("f")
            .containsExactly("bar");

        List<Map<String, Object>> created = submitAndGet(
            "MATCH (n {foo: \"bar\"}) WHERE NOT EXISTS(n.property) RETURN COUNT(n) AS ns"
        );

        assertThat(created)
            .extracting("ns")
            .containsExactly(1L);
    }

    @Test
    public void createListProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet("CREATE (n {foo: [1, 2, 3]}) RETURN n.foo AS f");

        assertThat(results)
            .extracting("f")
            .containsExactly(asList(1L, 2L, 3L));
    }

    @Test
    public void createMapProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (n {foo: {foo: 'bar', baz: 'qux'}}) RETURN n.foo AS f"
        );

        assertThat(results)
            .hasSize(1)
            .extracting("f")
            .extracting("foo", "baz")
            .containsExactly(tuple("bar", "qux"));
    }

    @Test
    public void unwindCreate() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND [3, 7, 11] AS i " +
                "CREATE (n {value: i}) " +
                "RETURN n.value"
        );

        assertThat(results)
            .extracting("n.value")
            .containsExactlyInAnyOrder(3L, 7L, 11L);
    }

    @Test
    public void unwindRangeCreate() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND range(3, 12, 4) AS i " +
                "CREATE (n {value: i}) " +
                "RETURN n.value"
        );

        assertThat(results)
            .extracting("n.value")
            .containsExactlyInAnyOrder(3L, 7L, 11L);
    }

    @Test
    public void matchCreateWithExisting() throws Exception {
        submitAndGet("CREATE (:Begin)");
        List<Map<String, Object>> begin = submitAndGet(
            "MATCH (n:Begin) RETURN COUNT(n) AS ns"
        );

        assertThat(begin)
            .extracting("ns")
            .containsExactly(1L);

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (x:Begin) CREATE (x)-[e:TYPE]->(v:End) RETURN e, v"
        );

        assertThat(results)
            .extracting("e")
            .extracting("label")
            .containsExactly("TYPE");

        assertThat(results)
            .extracting("e")
            .extracting("outVertex")
            .extracting("label")
            .containsExactly("Begin");

        assertThat(results)
            .extracting("e")
            .extracting("inVertex")
            .extracting("label")
            .containsExactly("End");

        assertThat(results)
            .extracting("v")
            .extracting("label")
            .containsExactly("End");
    }

    @Test
    public void createRelationshipInvalidSyntax() throws Exception {
        assertThatThrownBy(() -> submitAndGet("CREATE ({id: 2})-[r:KNOWS]-({id: 1}) RETURN r"))
            .satisfies(t -> {
                Throwable initialCause = getInitialCause(t);
                assertThat(initialCause).isInstanceOf(ResponseException.class);
                ResponseException ex = (ResponseException) initialCause;
                assertThat(ex.getRemoteStackTrace()).hasValueSatisfying(stackTrace ->
                    assertThat(stackTrace).contains("SyntaxException")
                );
            });
    }

    @Test
    public void createNodeAlreadyBound() throws Exception {
        assertThatThrownBy(() -> submitAndGet("CREATE (n:Foo)\n" +
            "CREATE (n:Bar)-[:OWNS]->(:Dog)"))
            .satisfies(t -> {
                Throwable initialCause = getInitialCause(t);
                assertThat(initialCause).isInstanceOf(ResponseException.class);
                ResponseException ex = (ResponseException) initialCause;
                assertThat(ex.getRemoteStackTrace()).hasValueSatisfying(stackTrace ->
                    assertThat(stackTrace).contains("SyntaxException")
                );
            });
    }

    private static Throwable getInitialCause(Throwable throwable) {
        Throwable lastThrowable;
        do {
            lastThrowable = throwable;
            throwable = throwable.getCause();
        } while (throwable != null);
        return lastThrowable;
    }
}
