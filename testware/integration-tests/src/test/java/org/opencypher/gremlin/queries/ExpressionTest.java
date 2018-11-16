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
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;

public class ExpressionTest {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    private List<Map<String, Object>> submitAndGet(String cypher, Map<String, ?> parameters) {
        return gremlinServer.cypherGremlinClient().submit(cypher, parameters).all();
    }

    @Test
    public void comparison() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("i1", 1L);
        args.put("i1_", 1L);
        args.put("i2", 2L);
        args.put("n", null);

        Map<String, Boolean> tests = new LinkedHashMap<>();
        tests.put("$i1 = $i1_", true);
        tests.put("$i1 = $i2", false);
        tests.put("$i1 = $n", null);
        tests.put("$n = $i1", null);

        tests.put("$i1 <> $i1_", false);
        tests.put("$i1 <> $i2", true);
        tests.put("$i1 <> $n", null);
        tests.put("$n <> $i1", null);

        tests.put("$i1 < $i1_", false);
        tests.put("$i1 < $i2", true);
        tests.put("$i2 < $i1", false);
        tests.put("$i1 < $n", null);
        tests.put("$n < $i1", null);

        tests.put("$i1 <= $i1_", true);
        tests.put("$i1 <= $i2", true);
        tests.put("$i2 <= $i1", false);
        tests.put("$i1 <= $n", null);
        tests.put("$n <= $i1", null);

        tests.put("$i1 > $i1_", false);
        tests.put("$i1 > $i2", false);
        tests.put("$i2 > $i1", true);
        tests.put("$i1 > $n", null);
        tests.put("$n > $i1", null);

        tests.put("$i1 >= $i1_", true);
        tests.put("$i1 >= $i2", false);
        tests.put("$i2 >= $i1", true);
        tests.put("$i1 >= $n", null);
        tests.put("$n >= $i1", null);

        runExpressionTests(args, tests);
    }

    @Test
    @Category(UsesExtensions.CustomPredicates.class)
    public void stringComparison() {
        Map<String, Object> args = new HashMap<>();
        args.put("s", "abc");
        args.put("sa", "a");
        args.put("sb", "b");
        args.put("sc", "c");
        args.put("sd", "d");
        args.put("n", null);

        Map<String, Boolean> tests = new LinkedHashMap<>();
        tests.put("$s STARTS WITH $sa", true);
        tests.put("$s CONTAINS $sb", true);
        tests.put("$s ENDS WITH $sc", true);
        tests.put("$s STARTS WITH $sc", false);
        tests.put("$s CONTAINS $sd", false);
        tests.put("$s ENDS WITH $sa", false);
        tests.put("$s STARTS WITH $n", null);
        tests.put("$s CONTAINS $n", null);
        tests.put("$s ENDS WITH $n", null);
        tests.put("$n STARTS WITH $sa", null);
        tests.put("$n CONTAINS $sa", null);
        tests.put("$n ENDS WITH $sa", null);

        runExpressionTests(args, tests);
    }

    @Test
    public void membership() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("l", asList(1L, 2L));
        args.put("le", emptyList());
        args.put("ln", asList(null, 2L));
        args.put("i2", 2L);
        args.put("i3", 3L);
        args.put("n", null);

        Map<String, Boolean> tests = new LinkedHashMap<>();
        tests.put("$i2 IN $l", true);
        tests.put("$i3 IN $l", false);
        tests.put("NOT $i2 IN $l", false);
        tests.put("NOT $i3 IN $l", true);

        tests.put("$n IN $l", null);
        tests.put("$n IN $le", false);
        tests.put("NOT $n IN $le", true);
        tests.put("$i2 IN $n", null);
        tests.put("$i2 IN $ln", true);
        tests.put("NOT $i2 IN $ln", false);
        tests.put("$i3 IN $ln", null);
        tests.put("$n IN $ln", null);

        runExpressionTests(args, tests);
    }

    @Test
    public void ternaryLogic() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("t", true);
        args.put("t2", true);
        args.put("f", false);
        args.put("f2", false);
        args.put("n", null);
        args.put("n2", null);

        Map<String, Boolean> tests = new LinkedHashMap<>();
        tests.put("NOT $n", null);
        tests.put("NOT $t", false);
        tests.put("NOT $f", true);

        tests.put("$t AND $t2", true);
        tests.put("$t AND $f", false);
        tests.put("$f AND $f2", false);
        tests.put("$t AND $n", null);
        tests.put("$f AND $n", false);

        tests.put("$t OR $t2", true);
        tests.put("$t OR $f", true);
        tests.put("$f OR $f2", false);
        tests.put("$t OR $n", true);
        tests.put("$f OR $n", null);

        tests.put("$t XOR $t2", false);
        tests.put("$f XOR $f2", false);
        tests.put("$t XOR $f", true);
        tests.put("$f XOR $t", true);
        tests.put("$n XOR $n2", null);
        tests.put("$n XOR $t", null);
        tests.put("$t XOR $n", null);
        tests.put("$n XOR $f", null);
        tests.put("$f XOR $n", null);

        runExpressionTests(args, tests);
    }

    private void runExpressionTests(Map<String, Object> args, Map<String, Boolean> tests) {
        for (Map.Entry<String, Boolean> entry : tests.entrySet()) {
            String expr = entry.getKey();
            Boolean result = entry.getValue();
            List<Map<String, Object>> results = submitAndGet("RETURN " + expr, args);

            assertThat(results)
                .extracting(expr)
                .containsExactly(result);
        }
    }

}
