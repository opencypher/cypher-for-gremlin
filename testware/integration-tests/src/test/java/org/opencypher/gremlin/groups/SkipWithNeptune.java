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
package org.opencypher.gremlin.groups;

import org.opencypher.gremlin.queries.LiteralTest;
import org.opencypher.gremlin.queries.SpecificsTest;
import org.opencypher.gremlin.translation.ir.rewrite.NeptuneFlavor;
import scala.collection.Seq;

/**
 * Tests that are skipped because of Neptune specifics. Refer to categories for more details.
 */
public interface SkipWithNeptune {
    /**
     * Steps are not always executed in traversals that ends on `.barrier().limit(0)`.
     *
     * @see SpecificsTest#limit0Gremlin()
     * @see SpecificsTest#limit0()
     * @see NeptuneFlavor#limit0Workaround(Seq) Workaround
     */
    interface Limit0 extends SkipWithNeptune {
    }

    /**
     * Count step traversal is not aliased.
     *
     * @see SpecificsTest#countStepIsNotAliasedGremlin()
     * @see SpecificsTest#countStepIsNotAliased()
     * @see NeptuneFlavor#barrierAfterCountWorkaround(Seq) Workaround
     */
    interface CountStepIsNotAliased extends SkipWithNeptune {
    }

    /**
     * Aggregating edges with same name does not work, only last aggregation is returned
     *
     * @see SpecificsTest#aggregateWithSameNameGremlin()
     * @see SpecificsTest#aggregateWithSameName
     * @see NeptuneFlavor#aggregateWithSameNameWorkaround(scala.collection.Seq) Workaround
     */
    interface AggregateWithSameName extends SkipWithNeptune {
    }

    /**
     * Some queries throw exceptions without details
     *
     * @see SpecificsTest#noExceptionDetailMessage()
     */
    interface NoExceptionDetailMessage extends SkipWithNeptune {
    }

    /**
     * `[:]` is not recognized as empty map
     *
     * @see LiteralTest#returnEmptyMap()
     */
    interface EmptyMap extends SkipWithNeptune {
    }
}
