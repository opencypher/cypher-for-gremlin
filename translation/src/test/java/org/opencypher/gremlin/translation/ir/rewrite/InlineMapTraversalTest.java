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

import static org.opencypher.gremlin.translation.CypherAstWrapper.parse;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssert.__;
import static org.opencypher.gremlin.translation.helpers.CypherAstAssertions.assertThat;

import org.junit.Test;
import org.opencypher.gremlin.translation.GremlinSteps;
import org.opencypher.gremlin.translation.ir.model.GremlinPredicate;
import org.opencypher.gremlin.translation.ir.model.GremlinStep;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;
import scala.collection.Seq;

public class InlineMapTraversalTest {

    @Test
    public void inlineProjectionMap() {
        GremlinSteps<Seq<GremlinStep>, GremlinPredicate> projection =
            __().project("n").by(__().constant(1));

        assertThat(parse(
            "WITH 1 AS n " +
                "RETURN n"
        ))
            .withFlavor(TranslatorFlavor.empty())
            .rewritingWith(InlineMapTraversal$.MODULE$)
            .removes(
                __().map(projection))
            .keeps(
                projection
            );
    }

    @Test
    public void adjacentMap() {
        assertThat(parse(
            "MATCH (n) " +
                "WHERE (n)-->(:L) " +
                "RETURN n"
        ))
            .withFlavor(TranslatorFlavor.empty())
            .rewritingWith(InlineMapTraversal$.MODULE$)
            .removes(__().select("n").map(__()).as("  cypher.path.start.GENERATED1").map(__().outE().inV()))
            .adds(__().select("n").as("  cypher.path.start.GENERATED1").outE().inV());
    }
}
