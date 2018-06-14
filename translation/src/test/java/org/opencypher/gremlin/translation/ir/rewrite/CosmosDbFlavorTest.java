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
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import org.junit.Test;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class CosmosDbFlavorTest {

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(
            InlineMapTraversal$.MODULE$,
            RemoveUselessSteps$.MODULE$
        ),
        seq()
    );

    @Test
    public void values() {
        assertThat(parse(
            "MATCH (n:N) " +
                "RETURN n.p"
        ))
            .withFlavor(flavor)
            .rewritingWith(CosmosDbFlavor$.MODULE$)
            .removes(
                __().values("p"))
            .adds(
                __().properties().hasKey("p").value());
    }

    @Test
    public void loops() {
        assertThat(parse(
            "UNWIND range(1, 3) AS i " +
                "RETURN i"
        ))
            .withFlavor(flavor)
            .rewritingWith(CosmosDbFlavor$.MODULE$)
            .removes(
                __().repeat(__().loops()
                    .aggregate("  GENERATED1"))
                    .times(4)
                    .cap("  GENERATED1")
                    .unfold()
                    .skip(1)
                    .limit(3)
            )
            .adds(
                __().inject(1, 2, 3));
    }
}
