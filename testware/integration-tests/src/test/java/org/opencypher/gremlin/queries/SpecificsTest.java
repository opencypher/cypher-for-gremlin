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
import static org.opencypher.gremlin.test.GremlinExtractors.byElementProperty;
import static org.opencypher.gremlin.test.TestCommons.DELETE_ALL;
import static org.opencypher.gremlin.translation.ReturnProperties.NODE_TYPE;
import static org.opencypher.gremlin.translation.ReturnProperties.RELATIONSHIP_TYPE;
import static org.opencypher.gremlin.translation.ReturnProperties.TYPE;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.SkipWithJanusGraph;
import org.opencypher.gremlin.groups.SkipWithNeptune;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.ir.rewrite.NeptuneFlavor;
import scala.collection.Seq;

public class SpecificsTest {

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
    public void return100Elements() throws Exception {
        Client client = gremlinServer.gremlinClient();

        client.submit("g.inject(1).times(100).repeat(addV().property('the_property', 'the_value'))").all().get();

        List<Result> results = client.submit("g.V()").all().get();

        assertThat(results)
            .hasSize(100);
    }

    @Test
    @Category(SkipWithJanusGraph.ChangePropertyType.class)
    public void changePropertyType() throws Exception {
        submitAndGet("CREATE (a {prop: 'the_prop'})");

        List<Map<String, Object>> results = submitAndGet("CREATE (b:New {prop: 1}) RETURN b.prop");

        assertThat(results)
            .extracting("b.prop")
            .containsExactlyInAnyOrder(1L);
    }

    @Test
    @Category(SkipWithJanusGraph.ChangePropertyType.class)
    public void changePropertyType2() throws Exception {
        List<Map<String, Object>> results = submitAndGet(
            "CREATE (a {prop: 'string'})\n" +
                "SET a.prop = 1\n" +
                "SET a.prop = 3.14\n" +
                "SET a.prop = true\n" +
                "RETURN a.prop"
        );

        assertThat(results)
            .extracting("a.prop")
            .containsExactlyInAnyOrder(true);
    }

    @Test
    @Category(SkipWithJanusGraph.ChangePropertyType.class)
    public void setAndGetEdgeProperty() throws Exception {
        Client client = gremlinServer.gremlinClient();

        Long count = client.submit("g.addV().as('n').addV().as('m').addE('REL').from('n').to('m').count()").one().getLong();
        assertThat(count).isEqualTo(1L);

        List<Result> results = client.submit("g.E().as('r').hasLabel('REL').property('name', 'neo4j').select('r').values('name')").all().get();
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getString()).isEqualTo("neo4j");
    }

    /**
     * @see #limit0()
     */
    @Test
    @Category(SkipWithNeptune.Limit0.class)
    public void limit0Gremlin() throws Exception {
        Client client = gremlinServer.gremlinClient();

        Long count = client.submit("g.addV().property('name', 'marko').addV().property('name', 'vadas').count()").one().getLong();
        assertThat(count).isEqualTo(1L);

        client.submit(
            "g.V().as('marko').has('name', eq('marko'))" +
                ".V().as('vadas').has('name', eq('vadas'))" +
                ".addE('knows').from('marko').to('vadas')" +
                ".barrier().limit(0)").all().get();

        long edges = client.submit("g.E().count()").one().getLong();

        assertThat(edges).isEqualTo(1);
    }

    /**
     * @see NeptuneFlavor#limit0Workaround(Seq)
     */
    @Test
    public void limit0() throws Exception {
        submitAndGet("CREATE ({name: 'marko'}), ({name: 'vadas'})");

        submitAndGet("MATCH (m {name: 'marko'}), (v {name: 'vadas'}) " +
            "CREATE (m)-[:knows]->(v)");

        List<Map<String, Object>> results = submitAndGet("MATCH ()-[r]->() RETURN count(r) as r");

        assertThat(results)
            .extracting("r")
            .containsExactly(1L);
    }

    /**
     * @see #aggregateWithSameName()
     */
    @Test
    @Category(SkipWithNeptune.AggregateWithSameName.class)
    public void aggregateWithSameNameGremlin() throws Exception {
        Client client = gremlinServer.gremlinClient();

        Long count = client.submit(
            "g.addV('X').as('x')" +
                ".addV().as('n1')" +
                ".addV().as('n2')" +
                ".addE('R1').from('x').to('n1')" +
                ".addE('R2').from('n1').to('n2')" +
                ".count()").one().getLong();
        assertThat(count).isEqualTo(1L);

        List<Result> results = client.submit(
            "g.V().hasLabel('X').outE().aggregate('x')" +
                ".inV().outE().aggregate('x')" +
                ".cap('x').unfold().label()").all().get();

        assertThat(results)
            .extracting(Result::getString)
            .containsExactlyInAnyOrder("R1", "R2");
    }

    /**
     * @see NeptuneFlavor#aggregateWithSameNameWorkaround(scala.collection.Seq)
     */
    @Test
    public void aggregateWithSameName() throws Exception {
        submitAndGet("CREATE (:X)-[:R1]->(:n1)-[:R2]->(:n2)");

        List<Map<String, Object>> results = submitAndGet("MATCH x = (:X)-->()-->() RETURN x");


        assertThat(results)
            .flatExtracting("x")
            .extracting(byElementProperty(TYPE))
            .containsExactlyInAnyOrder(NODE_TYPE, RELATIONSHIP_TYPE, NODE_TYPE, RELATIONSHIP_TYPE, NODE_TYPE);
    }

    /**
     * @see #aggregateWithSameName()
     */
    @Test
    @Category(SkipWithNeptune.CountStepIsNotAliased.class)
    public void countStepIsNotAliasedGremlin() throws Exception {
        Client client = gremlinServer.gremlinClient();

        client.submit("g.addV().addV()").all().get();

        List<Result> results = client.submit("g.V().count().as('a').select('a')").all().get();

        assertThat(results)
            .extracting(Result::getLong)
            .containsExactlyInAnyOrder(2L);
    }

    /**
     * @see NeptuneFlavor#barrierAfterCountWorkaround(Seq)
     */
    @Test
    public void countStepIsNotAliased() throws Exception {
        submitAndGet("CREATE (), ()");

        List<Map<String, Object>> results = submitAndGet("MATCH (a) RETURN count(a) > 0 as r");

        assertThat(results)

            .extracting("r")
            .containsExactlyInAnyOrder(true);
    }

    @Test
    @Category(SkipWithNeptune.NoExceptionDetailMessage.class)
    public void noExceptionDetailMessage() throws Exception {
        Client client = gremlinServer.gremlinClient();

        assertThatThrownBy(() -> client.submit("g.inject(1).math('_ / 0')").all().get())
            .hasMessageContaining("Division by zero");
    }
}
