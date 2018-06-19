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
package org.opencypher.gremlin.translation.ir.rewrite;

import static org.opencypher.gremlin.translation.CypherAst.parse;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssert.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.translator.TranslatorFlavor.empty;

import org.junit.Test;

public class RemoveUnusedAliasesTest {

    @Test
    public void generated() {
        assertThat(parse(
            "MATCH (n)-->() " +
                "RETURN n"
        ))
            .withFlavor(empty())
            .rewritingWith(RemoveUnusedAliases$.MODULE$)
            .removes(__().as("  UNNAMED10"))
            .removes(__().as("  UNNAMED13"))
            .keeps(__().as("n"));
    }

    @Test
    public void explicit() {
        assertThat(parse(
            "MATCH (n)-[r]->(m) " +
                "RETURN n"
        ))
            .withFlavor(empty())
            .rewritingWith(RemoveUnusedAliases$.MODULE$)
            .removes(__().as("r"))
            .removes(__().as("m"))
            .keeps(__().as("n"));
    }

    @Test
    public void fromTo() {
        assertThat(parse(
            "CREATE (n)-[:R]->(m)"
        ))
            .withFlavor(empty())
            .rewritingWith(RemoveUnusedAliases$.MODULE$)
            .removes(__().as("  UNNAMED11"))
            .keeps(__().as("n"))
            .keeps(__().as("m"));
    }

    @Test
    public void reAlias() {
        assertThat(parse(
            "MATCH (n)-->(m) " +
                "MATCH (m)-->(k) " +
                "RETURN n"
        ))
            .withFlavor(empty())
            .rewritingWith(RemoveUnusedAliases$.MODULE$)
            .removes(__().as("  UNNAMED10"))
            .removes(__().as("  UNNAMED26"))
            .keeps(__().as("n"))
            .keeps(__().as("m"));
    }

}
