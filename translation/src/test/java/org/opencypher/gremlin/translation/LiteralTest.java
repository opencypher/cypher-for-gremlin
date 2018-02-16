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

import org.junit.Before;
import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.opencypher.gremlin.translation.Tokens.NULL;
import static org.opencypher.gremlin.translation.Tokens.START;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

public class LiteralTest {

    private List<String> primitiveList;
    private List<String> literalList;
    private List<? extends Serializable> nestedList;

    @Before
    public void setUp() {
        primitiveList = asList(
            "13",
            "-40000",
            "3.14",
            "6.022E23",
            "'Hello'",
            "\"World\"",
            "true",
            "false",
            "TRUE",
            "FALSE",
            "null"
        );

        literalList = new ArrayList<>();
        literalList.addAll(primitiveList);
        literalList.addAll(asList(
            "[]",
            "[13, 'Hello', true, null]"
        ));

        nestedList = asList(
            13,
            "Hello",
            true,
            Tokens.NULL
        );
    }

    @Test
    public void literalsInCreate() {
        String fieldMap = IntStream.range(0, literalList.size())
            .mapToObj(i -> new SimpleEntry<>(i, literalList.get(i)))
            .map(e -> "n" + e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", "));
        assertThat(parse(
            "CREATE (n:L {" + fieldMap + "})"
        ))
            .hasTraversalBeforeReturn(
                __.addV("L").as("n")
                    .property("n0", 13)
                    .property("n1", -40000)
                    .property("n2", 3.14)
                    .property("n3", 6.022E23)
                    .property("n4", "Hello")
                    .property("n5", "World")
                    .property("n6", true)
                    .property("n7", false)
                    .property("n8", true)
                    .property("n9", false)
                    .sideEffect(__.properties("n11").drop())
                    .property("n12", nestedList)
                    .barrier().limit(0)
            );
    }

    @Test
    public void literalsInWith() {
        String literals = String.join(", ", literalList);
        assertThat(parse(
            "WITH [" + literals + "] AS list\n" +
                "RETURN list"
        ))
            .hasTraversalBeforeReturn(
                __.inject(START)
                    .constant(asList(
                        13,
                        -40000,
                        3.14,
                        6.022E23,
                        "Hello",
                        "World",
                        true,
                        false,
                        true,
                        false,
                        Tokens.NULL,
                        emptyList(),
                        nestedList
                    ))
                    .limit(1).as("list")
                    .select("list")
            );
    }

    @Test
    public void literalsInUnwind() {
        String literals = String.join(", ", literalList);
        assertThat(parse(
            "UNWIND [" + literals + "] AS list\n" +
                "RETURN list"
        ))
            .hasTraversalBeforeReturn(
                __.inject(
                    13,
                    -40000,
                    3.14,
                    6.022E23,
                    "Hello",
                    "World",
                    true,
                    false,
                    true,
                    false,
                    Tokens.NULL,
                    emptyList(),
                    nestedList
                )
                    .as("list")
                    .select("list")
            );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void literalsInReturnList() {
        String literals = String.join(", ", literalList);
        assertThat(parse(
            "RETURN [" + literals + "] AS list"
        ))
            .hasTraversal(
                __.inject(START)
                    .coalesce(__
                        .project("list")
                        .by(__.constant(asList(
                            13,
                            -40000,
                            3.14,
                            6.022E23,
                            "Hello",
                            "World",
                            true,
                            false,
                            true,
                            false,
                            NULL,
                            emptyList(),
                            nestedList
                        )))
                    )
            );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void literalsInReturn() {
        String literals = IntStream.range(0, primitiveList.size())
            .mapToObj(i -> new SimpleEntry<>(i, primitiveList.get(i)))
            .map(e -> String.format("%s AS n%d", e.getValue(), e.getKey()))
            .collect(Collectors.joining(", "));
        assertThat(parse(
            "RETURN " + literals
        ))
            .hasTraversal(
                __.inject(START)
                    .coalesce(__
                        .project("n0", "n1", "n2", "n3", "n4", "n5", "n6", "n7", "n8", "n9", "n10")
                        .by(__.constant(13))
                        .by(__.constant(-40000))
                        .by(__.constant(3.14))
                        .by(__.constant(6.022E23))
                        .by(__.constant("Hello"))
                        .by(__.constant("World"))
                        .by(__.constant(true))
                        .by(__.constant(false))
                        .by(__.constant(true))
                        .by(__.constant(false))
                        .by(__.constant(NULL))
                    )
            );
    }

}
