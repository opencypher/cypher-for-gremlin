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

import static java.util.Collections.emptyMap;
import static org.opencypher.gremlin.translation.CypherAst.parse;
import static org.opencypher.gremlin.translation.Tokens.NULL;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssert.P;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssert.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;
import static org.opencypher.gremlin.translation.helpers.ScalaHelpers.seq;

import java.util.ArrayList;
import java.util.List;
import org.apache.tinkerpop.gremlin.structure.Column;
import org.junit.Test;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class SimplifyPropertySettersTest {

    private static final List<Object> EMPTY_LIST = new ArrayList<>();

    private final TranslatorFlavor flavor = new TranslatorFlavor(
        seq(
            InlineMapTraversal$.MODULE$
        ),
        seq()
    );

    @Test
    public void createProperties() {
        assertThat(parse(
            "CREATE ({foo: 'bar', baz: null, quux: $x})"
        ))
            .withFlavor(flavor)
            .rewritingWith(SimplifyPropertySetters$.MODULE$)
            .removes(
                __().property("foo", __().constant("bar")))
            .adds(
                __().property("foo", "bar"));
    }

    @Test
    public void setProperties() {
        assertThat(parse(
            "MATCH (n) " +
                "SET " +
                "n.p1 = []," +
                "n.p2 = [1]," +
                "n.p3 = {}," +
                "n.p4 = {k:1}"
        ))
            .withFlavor(flavor)
            .rewritingWith(SimplifyPropertySetters$.MODULE$)
            .removes(
                __().choose(
                    __().constant(EMPTY_LIST).is(P.neq(NULL)).unfold(),
                    __().property("p1", __().constant(EMPTY_LIST)),
                    __().sideEffect(__().properties("p1").drop())
                )
            )
            .keeps(
                __().sideEffect(__().properties("p1").drop()))
            .keeps(
                __().property("p2", __().project("  GENERATED1").by(__().constant(1)).select(Column.values)))
            .adds(
                __().property("p3", emptyMap()))
            .keeps(
                __().property("p4", __().project("k").by(__().constant(1))));
    }
}
