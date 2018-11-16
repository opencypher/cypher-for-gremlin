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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.opencypher.gremlin.test.TestCommons.parameterMap;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.test.TestCommons;

public class DeleteTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource(TestCommons::modernGraph);

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return gremlinServer.cypherGremlinClient().submit(cypher).all();
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Object... parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameterMap(parameters)).all();
    }

    @Before
    public void setUp() throws Exception {
        TestCommons.modernGraph(gremlinServer.cypherGremlinClient());
    }

    @Test
    public void delete() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (s:software) RETURN count(s)"
        );

        assertThatThrownBy(() -> submitAndGet("MATCH (s:software) DELETE s"))
            .hasMessageContaining("Cannot delete node, because it still has relationships.");

        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (s:software) DETACH DELETE s"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (s:software) RETURN count(s)"
        );

        assertThat(beforeDelete)
            .extracting("count(s)")
            .containsExactly(2L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(s)")
            .containsExactly(0L);
    }

    @Test
    public void deleteMultiple() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThatThrownBy(() -> submitAndGet("MATCH (s:software), (p:person) DELETE s, p"))
            .hasMessageContaining("Cannot delete node, because it still has relationships.");

        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (s:software), (p:person) DETACH DELETE s, p"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }

    @Test
    public void detachDeletePaths() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH p = (:software)<-[:created]-() DETACH DELETE p"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(1L);
    }

    @Test
    public void deletePath() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThatThrownBy(() -> submitAndGet("MATCH (s:software) DELETE s"))
            .hasMessageContaining("Cannot delete node, because it still has relationships.");

        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH p = (:person {name: 'marko'})-[:created]->() DETACH DELETE p"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(4L);
    }

    @Test
    public void deleteSingleLongPath() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH p = (:person {name: 'peter'})-[:created]->()<-[:created]-(:person)-[:created]->() " +
                "DETACH DELETE p"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(2L);
    }

    @Test
    public void detachDeleteFromAList() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThatThrownBy(() -> submitAndGet("MATCH (n) " +
                        "WITH [n] AS nodes " +
                        "DELETE nodes[0]"))
            .hasMessageContaining("Cannot delete node, because it still has relationships.");

        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (n) " +
                "WITH [n] AS nodes " +
                "DETACH DELETE nodes[0]"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }

    @Test
    public void deleteWithReturn() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (a:software) DETACH DELETE a RETURN a"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .extracting("a")
            .extracting("name")
            .containsExactlyInAnyOrder("lop", "ripple");
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(4L);
    }

    @Test
    public void deleteWithReturnOther() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (a:software)<-[:created]-(p:person) DETACH DELETE a RETURN p"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .extracting("p")
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "josh", "josh", "peter");
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(4L);
    }

    @Test
    public void deleteWithAggregationOnField() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (a:software)<-[:created]-(p:person) DETACH DELETE a RETURN p.name, count(*)"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .extracting("p.name", "count(*)")
            .containsExactlyInAnyOrder(
                tuple("marko", 1L),
                tuple("josh", 2L),
                tuple("peter", 1L));
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(4L);
    }

    @Test
    public void deleteNothing() throws Exception {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (a:notExisting) DELETE a RETURN a"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .hasSize(0);
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(6L);
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void deleteWithTypeLost() throws Exception {
        assertThatThrownBy(() -> submitAndGet(
            "MATCH (n) WITH collect(n) as typelost\n" +
                "DELETE typelost[$i]",
            "i",
            0

        )).hasMessageContaining("Cannot delete node, because it still has relationships.");
    }

    @Test
    public void deleteWithReturnAggregation() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");
        submitAndGet("CREATE ()-[:R]->()");
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (a)-[r]-(b) DELETE r, a, b RETURN count(*)"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(2L);
        assertThat(onDelete)
            .extracting("count(*)")
            .containsExactly(2L);
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }

    @Test
    public void deleteOptionalMatch() throws Exception {
        submitAndGet("MATCH (n) DETACH DELETE n");
        submitAndGet("CREATE ()");
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        submitAndGet(
            "MATCH (n)\n" +
                "OPTIONAL MATCH (n)-[r]-()\n" +
                "DELETE r, n"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(1L);
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(0L);
    }

    @Test
    public void deleteOnNullNode() throws Exception {
        submitAndGet(
            "MATCH (n) DETACH DELETE n"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "OPTIONAL MATCH (n) DELETE n"
        );

        assertThat(onDelete)
            .isEmpty();
    }

    @Test
    public void deleteConnectedNodeAndRelationship() {
        List<Map<String, Object>> beforeDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );
        List<Map<String, Object>> onDelete = submitAndGet(
            "MATCH (s:software {name: 'ripple'})<-[r:created]-(p:person) DELETE s, r"
        );
        List<Map<String, Object>> afterDelete = submitAndGet(
            "MATCH (n) RETURN count(*)"
        );

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .isEmpty();
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(5L);
    }

    @Test
    public void failOnPropertyAccess() {
        Object beforeDelete = submitAndGet("MATCH (n) RETURN count(*)").get(0).get("count(*)");

        assertThatThrownBy(() -> submitAndGet("MATCH (n) DELETE n RETURN n.name"))
            .hasMessageContaining("Deleted entity property access");

        assertThatThrownBy(() -> submitAndGet("MATCH (n) DELETE n RETURN labels(n)"))
            .hasMessageContaining("Deleted entity label access");

        assertThatThrownBy(() -> submitAndGet("MATCH (n:person)-[r]->(a:software) " +
            "DELETE n " +
            "RETURN a.name, n.name"))
            .hasMessageContaining("Deleted entity property access");

        Object afterDelete = submitAndGet("MATCH (n) RETURN count(*)").get(0).get("count(*)");

        assertThat(beforeDelete).isEqualTo(afterDelete);
    }

    @Test
    public void dontFailOnUnrelatedPropertyAccess() {
        List<Map<String, Object>> beforeDelete = submitAndGet("MATCH (n) RETURN count(*)");

        List<Map<String, Object>> onDelete = submitAndGet("MATCH (n:person)-[r]->(a:software) DETACH DELETE n RETURN a.name");

        List<Map<String, Object>> afterDelete = submitAndGet("MATCH (n) RETURN count(*)");

        assertThat(beforeDelete)
            .extracting("count(*)")
            .containsExactly(6L);
        assertThat(onDelete)
            .extracting("a.name")
                    .containsExactlyInAnyOrder("lop", "lop", "lop", "ripple");
        assertThat(afterDelete)
            .extracting("count(*)")
            .containsExactly(3L);
    }

}
