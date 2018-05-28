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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.extension.TestProcedures;
import org.opencypher.gremlin.groups.SkipWithBytecode;
import org.opencypher.gremlin.groups.SkipWithGremlinGroovy;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

/**
 * @see TestProcedures
 */
@Category({
    SkipWithBytecode.class,
    SkipWithGremlinGroovy.class
})
public class ProcedureTest {

    private GraphTraversalSource gts = TinkerGraph.open().traversal();
    private TestProcedures testProcedures = new TestProcedures();

    private List<Map<String, Object>> submitAndGet(String cypher) {
        return submitAndGet(cypher, emptyMap());
    }

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        DefaultGraphTraversal g = new DefaultGraphTraversal(gts);
        Translator<GraphTraversal, P> translator = Translator.builder()
            .traversal(g)
            .procedures(testProcedures.get())
            .build();
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, parameters);
        GraphTraversal<?, ?> traversal = ast.buildTranslation(translator);
        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());
        return traversal.toStream()
            .map(returnNormalizer::normalize)
            .collect(toList());
    }

    @Test
    public void callYield() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.getName() " +
                "YIELD name AS out " +
                "RETURN out"
        );

        assertThat(results)
            .extracting("out")
            .containsExactlyInAnyOrder("marko", "vadas");
    }

    @Test
    public void callYieldWithArgument() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.inc(3) " +
                "YIELD r AS out " +
                "RETURN out"
        );

        assertThat(results)
            .extracting("out")
            .containsExactly(4L);
    }

    @Test
    public void callYieldNothing() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.void() " +
                "RETURN 1 AS out"
        );

        assertThat(results)
            .extracting("out")
            .containsExactly(1L);
    }

    @Test
    public void matchYieldNothing() {
        List<Map<String, Object>> results = submitAndGet(
            "UNWIND ['foo', 'bar'] AS r " +
                "CALL test.void() " +
                "RETURN r"
        );

        assertThat(results)
            .extracting("r")
            .containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    public void callYieldUnionTypeCasts() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.inc(2.0) YIELD r " +
                "UNION ALL " +
                "CALL test.incF(2) YIELD r "
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(3L, 3.0);
    }

    @Test
    public void standaloneCall() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.getName()"
        );

        assertThat(results)
            .extracting("name")
            .containsExactlyInAnyOrder("marko", "vadas");
    }

    @Test
    public void standaloneCallWithArgument() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.inc(2)"
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(3L);
    }

    @Test
    public void standaloneCallFailsOnWrongType() {
        Throwable throwable = catchThrowable(() -> submitAndGet(
            "CALL test.inc(true)"
        ));

        assertThat(throwable)
            .hasMessageContaining("Invalid argument types for test.inc");
    }

    @Test
    public void standaloneImplicitCall() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.inc",
            singletonMap("a", 1)
        );

        assertThat(results)
            .extracting("r")
            .containsExactly(2L);
    }

    @Test
    public void standaloneCallOrder() {
        List<Map<String, Object>> results = submitAndGet(
            "CALL test.multi"
        );

        assertThat(results)
            .flatExtracting(Map::values)
            .containsExactly("foo", "bar");
    }
}
