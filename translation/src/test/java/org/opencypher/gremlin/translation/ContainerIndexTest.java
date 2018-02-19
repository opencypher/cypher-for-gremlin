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

import static java.util.Arrays.asList;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.CypherAstHelpers.parse;

import org.junit.Test;
import org.opencypher.gremlin.translation.helpers.CypherAstAssertions.__;

public class ContainerIndexTest {

    @Test
    public void listIndexInReturn() {
        assertThat(parse(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN list[1] AS i"
        ))
            .hasTraversalBeforeReturn(
                __.inject(Tokens.START)
                    .constant(asList(1, 2, 3)).limit(1).as("list")
                    .select("list")
            );
    }

    @Test
    public void listIndexInReturnFunction() {
        assertThat(parse(
            "WITH [1, 2, 3] AS list\n" +
                "RETURN toString(list[1]) AS s"
        ))
            .hasTraversalBeforeReturn(
                __.inject(Tokens.START)
                    .constant(asList(1, 2, 3)).limit(1).as("list")
                    .select("list")
            );
    }
}
