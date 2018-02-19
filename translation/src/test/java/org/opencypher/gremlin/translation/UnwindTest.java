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

import static org.opencypher.gremlin.translation.Tokens.START;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

public class UnwindTest {

    @Test
    public void listCreate() throws Exception {
        assertThat(parse(
            "UNWIND [1, 2, 3] AS i " +
                "CREATE (n)"
        )).hasTraversalBeforeReturn(
            __
                .inject(1, 2, 3).as("i")
                .addV().as("n")
                .barrier().limit(0)
        );
    }

    @Test
    public void listCreateProperty() throws Exception {
        assertThat(parse(
            "UNWIND [1, 2, 3] AS i " +
                "CREATE (n {num: i})"
        )).hasTraversalBeforeReturn(
            __
                .inject(1, 2, 3).as("i")
                .addV().as("n")
                .property("num", __.select("i"))
                .barrier().limit(0)
        );
    }

    @Test
    public void rangeCreate() throws Exception {
        assertThat(parse(
            "UNWIND range(1, 3) AS i " +
                "CREATE (n)"
        )).hasTraversalBeforeReturn(
            __
                .inject(START).repeat(__.loops().aggregate("  GENERATED1")).times(4).cap("  GENERATED1")
                .unfold().range(1, 4).as("i")
                .addV().as("n")
                .barrier().limit(0)
        );
    }

    @Test
    public void inlineRangeCreate() throws Exception {
        assertThat(parse(
            "UNWIND range(1, 3) AS i " +
                "CREATE (n)"
        )).hasTraversalBeforeReturn(
            __
                .inject(START)
                .repeat(__.loops().aggregate("  GENERATED1"))
                .times(4)
                .cap("  GENERATED1")
                .unfold()
                .range(1, 4).as("i")
                .addV().as("n")
                .barrier().limit(0)
        );
    }

    @Test
    public void rangeStepCreate() throws Exception {
        assertThat(parse(
            "UNWIND range(1, 3, 2) AS i " +
                "CREATE (n)"
        )).hasTraversalBeforeReturn(
            __
                .inject(1, 3).as("i")
                .addV().as("n")
                .barrier().limit(0)
        );
    }

    @Test
    public void listReturn() throws Exception {
        assertThat(parse(
            "UNWIND [1, 2, 3] AS i " +
                "RETURN i"
        )).hasTraversalBeforeReturn(
            __
                .inject(1, 2, 3).as("i")
                .select("i")
        );
    }

    @Test
    public void listWithReturn() throws Exception {
        assertThat(parse(
            "UNWIND [1, 2, 3] AS i " +
                "WITH i AS x " +
                "RETURN x"
        )).hasTraversalBeforeReturn(
            __
                .inject(1, 2, 3).as("i")
                .select("i").as("x")
                .select("x")
        );
    }
}
