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
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;
import static org.opencypher.gremlin.translation.ReturnProperties.ID;
import static org.opencypher.gremlin.translation.ReturnProperties.INV;
import static org.opencypher.gremlin.translation.ReturnProperties.LABEL;
import static org.opencypher.gremlin.translation.ReturnProperties.OUTV;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.driver.exception.ResponseException;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesCollectionsInProperties;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class CreateTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Before
    public void setUp() {
        submitAndGet(DELETE_ALL);
    }

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
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
        Map root = (Map) result.get("root");
        Map link = (Map) result.get("link");
        assertThat(link.get(INV)).isEqualTo(root.get(ID));
        assertThat(link.get(OUTV)).isEqualTo(root.get(ID));
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
        Object rootId = ((Map) submitAndGet("CREATE (d:D) RETURN d").get(0).get("d")).get(ID);

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (d:D) " +
                "CREATE (e1:E {name:'0'}), (e2:E {name:'1'}) " +
                "CREATE (d)-[:MCREATE]->(e1), (d)-[:MCREATE]->(e2)"
        );

        assertThat(results).isEmpty();

        List<Object> createdIds = submitAndGet("MATCH (n:E) RETURN n")
            .stream()
            .map(m -> (Map) m.get("n"))
            .map(v -> v.get(ID))
            .collect(toList());

        assertThat(createdIds).hasSize(2);

        List<Map> matchCreates = submitAndGet("MATCH ()-[r:MCREATE]->() RETURN r")
            .stream()
            .map(m -> (Map) m.get("r"))
            .collect(toList());
        assertThat(matchCreates).hasSize(2);

        Map edge1 = matchCreates.get(0);
        assertThat(edge1.get(OUTV)).isEqualTo(rootId);
        assertThat(createdIds).contains(edge1.get(INV));

        Map edge2 = matchCreates.get(1);
        assertThat(edge2.get(OUTV)).isEqualTo(rootId);
        assertThat(createdIds).contains(edge2.get(INV));
    }

    @Test
    public void matchDeleteCreate() throws Exception {
        List<Map<String, Object>> toBeDeleted = submitAndGet("CREATE (a:A)-[r:TBDELETED]->(b:B) RETURN COUNT(r)");

        assertThat(toBeDeleted)
            .extracting("COUNT(r)")
            .containsExactly(1L);

        submitAndGet("MATCH (a:A)-[r]->(b)\n" +
            "DELETE r\n" +
            "CREATE (b)-[:RECREATED]->(a) RETURN b");

        toBeDeleted = submitAndGet("MATCH ()-[r:TBDELETED]->() RETURN COUNT(r)");

        assertThat(toBeDeleted)
            .extracting("COUNT(r)")
            .containsExactly(0L);

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
        Object markoId = ((Map) created.get("marko")).get(ID);
        Object vadasId = ((Map) created.get("vadas")).get(ID);

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (marko:person),(vadas:person) " +
                "WHERE marko.name = 'marko' AND vadas.name = 'vadas' " +
                "CREATE (marko)-[r:matchCreates]->(vadas)"
        );

        assertThat(results).isEmpty();

        Map edge = (Map) submitAndGet("MATCH ()-[r:matchCreates]->() RETURN r").get(0).get("r");

        assertThat(edge.get(OUTV)).isEqualTo(markoId);
        assertThat(edge.get(INV)).isEqualTo(vadasId);
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
    public void createNullPropertyOnNode() throws Exception {
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
    @SuppressWarnings("unchecked")
    public void createNullPropertyOnRelationship() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE ()-[r:X {foo: 'bar', property: null}]->() " +
                "RETURN r.foo"
        );

        assertThat(results)
            .extracting("r.foo")
            .containsExactly("bar");

        List<? extends Map<String, ?>> properties = gremlinServer.gremlinClient().alias("g").submit(
            __.V()
                .outE()
                .as("E")
                .properties()
                .project("key", "value")
                .by(__.key())
                .by(__.value())
        ).all().join().stream()
            .map(r -> (Traverser<Map<String, ?>>) r.getObject())
            .map(Traverser::get)
            .collect(toList());

        assertThat(properties)
            .extracting("key", "value")
            .containsExactly(tuple("foo", "bar"));
    }

    @Test
    @Category(UsesCollectionsInProperties.ListDataType.class)
    public void createListProperty() throws Exception {
        List<Map<String, Object>> results = submitAndGet("CREATE (n {foo: [1, 2, 3]}) RETURN n.foo AS f");

        assertThat(results)
            .extracting("f")
            .containsExactly(asList(1L, 2L, 3L));
    }

    @Test
    @Category({UsesCollectionsInProperties.MapDataType.class})
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
    public void withCreate() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "WITH 42 AS i " +
                "CREATE (n:N {value: i}) " +
                "RETURN n.value"
        );

        assertThat(results)
            .extracting("n.value")
            .containsExactly(42L);
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
            "UNWIND range(3, 7) AS i " +
                "CREATE (n {value: i}) " +
                "RETURN n.value"
        );

        assertThat(results)
            .extracting("n.value")
            .containsExactlyInAnyOrder(3L, 4L, 5L, 6L, 7L);
    }

    @Test
    public void unwindRangeStepCreate() throws Exception {
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
            "MATCH (n:Begin) RETURN id(n) as bid, COUNT(n) AS ns"
        );

        Object beginId = begin.get(0).get("bid");

        assertThat(begin)
            .extracting("ns")
            .containsExactly(1L);

        List<Map<String, Object>> results = submitAndGet(
            "MATCH (x:Begin) CREATE (x)-[e:TYPE]->(v:End) RETURN e, v"
        );

        assertThat(results)
            .extracting("v")
            .extracting(LABEL)
            .containsExactly("End");

        Object endId = ((Map) results.get(0).get("v")).get(ID);

        assertThat(results)
            .extracting("e")
            .extracting(LABEL)
            .containsExactly("TYPE");

        assertThat(results)
            .extracting("e")
            .extracting(OUTV)
            .containsExactly(beginId);

        assertThat(results)
            .extracting("e")
            .extracting(INV)
            .containsExactly(endId);
    }

    @Test
    public void createRelationshipInvalidSyntax() throws Exception {
        assertThatThrownBy(() -> submitAndGet("CREATE ({id: 2})-[r:KNOWS]-({id: 1}) RETURN r"))
            .satisfies(t -> {
                Optional<String> getStackTrace = getInitialCause(t);
                assertThat(getStackTrace).hasValueSatisfying(stackTrace ->
                    assertThat(stackTrace).contains("SyntaxException")
                );
            });
    }

    @Test
    public void createNodeAlreadyBound() throws Exception {
        assertThatThrownBy(() -> submitAndGet("CREATE (n:Foo)\n" +
            "CREATE (n:Bar)-[:OWNS]->(:Dog)"))
            .satisfies(t -> {
                Optional<String> getStackTrace = getInitialCause(t);
                assertThat(getStackTrace).hasValueSatisfying(stackTrace ->
                    assertThat(stackTrace).contains("SyntaxException")
                );
            });
    }

    @Test
    public void createNode() {
        assertThat(submitAndGet(
            "CREATE (marko:person {name: \"marko\", age: 29})"
        )).isEmpty();

        assertThat(submitAndGet("MATCH (n:person) RETURN n.name, n.age").get(0))
            .containsEntry("n.name", "marko")
            .containsEntry("n.age", 29L);
    }

    @Test
    @Category(UsesCollectionsInProperties.ListDataType.class)
    public void createNodeWithListProperty() throws Exception {
        assertThat(submitAndGet(
            "CREATE (n:L {foo: ['one', 'two', 'three']})"
        )).isEmpty();

        Map<String, Object> result = submitAndGet("MATCH (n:L) RETURN n.foo").get(0);
        assertThat(result).containsEntry("n.foo", asList("one", "two", "three"));
    }

    @Test
    public void createEmptyNode() {
        assertThat(submitAndGet(
            "CREATE (n)"
        )).isEmpty();

        Map<String, Object> result = submitAndGet(
            "MATCH (n) RETURN size(keys(n)) AS keys, COUNT(n) AS vertices"
        ).get(0);

        assertThat(result.get("keys")).isEqualTo(0L);
        assertThat(result.get("vertices")).isEqualTo(1L);
    }

    @Test
    public void multipleCreateNodes() {
        assertThat(submitAndGet(
            "CREATE (marko:person {name: \"marko\", age: 29}) " +
                "CREATE (vadas:person {name: \"vadas\", age: 27})"
        )).isEmpty();

        long vertices = (long) submitAndGet("MATCH (n) RETURN COUNT(n) AS vertices").get(0).get("vertices");
        assertThat(vertices).isEqualTo(2);
    }

    @Test
    public void createAndMatch() throws Exception {
        assertThat(submitAndGet(
            "CREATE (marko:person)-[r:knows]->(vadas:person) " +
                "WITH marko AS m " +
                "MATCH (m)-[r:knows]->(friend) " +
                "RETURN friend"
        )).hasSize(1);

        long vertices = (long) submitAndGet("MATCH (n) RETURN COUNT(n) as vertices").get(0).get("vertices");
        long edges = (long) submitAndGet("MATCH ()-[r]->() RETURN COUNT(r) as edges").get(0).get("edges");

        assertThat(vertices).isEqualTo(2L);
        assertThat(edges).isEqualTo(1L);
    }


    @Test
    public void matchAndCreate() {
        submitAndGet(
            "CREATE (marko:person {name: \"marko\"}) " +
                "CREATE (vadas:person {name: \"vadas\"})"
        );

        assertThat(submitAndGet(
            "MATCH (marko:person),(vadas:person) " +
                "WHERE marko.name = 'marko' AND vadas.name = 'vadas' " +
                "CREATE (marko)-[r:knows]->(vadas)"
        )).isEmpty();

        long vertices = (long) submitAndGet("MATCH (n) RETURN COUNT(n) as vertices").get(0).get("vertices");
        long edges = (long) submitAndGet("MATCH ()-[r]->() RETURN COUNT(r) as edges").get(0).get("edges");

        assertThat(vertices).isEqualTo(2L);
        assertThat(edges).isEqualTo(1L);
    }

    private static Optional<String> getInitialCause(Throwable throwable) {
        Throwable lastThrowable;
        do {
            lastThrowable = throwable;
            throwable = throwable.getCause();
        } while (throwable != null);

        if (lastThrowable instanceof ResponseException) {
            ResponseException ex = (ResponseException) lastThrowable;
            return ex.getRemoteStackTrace();
        } else {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            lastThrowable.printStackTrace(pw);
            return Optional.of(sw.toString());
        }
    }
}
