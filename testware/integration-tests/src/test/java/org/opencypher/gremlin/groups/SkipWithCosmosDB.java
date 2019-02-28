/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
import org.opencypher.gremlin.translation.ir.rewrite.CosmosDbFlavor;
import org.opencypher.gremlin.translation.ir.rewrite.CustomFunctionFallback;

/**
 * Tests that are skipped because of Cosmos DB specifics. Refer to categories for more details.
 */
public interface SkipWithCosmosDB {
    /**
     * from() and to() modulators are not supported for path() step
     *
     * @see SpecificsTest#pathFromToNotSupported()
     * @see CosmosDbFlavor#removeFromTo
     */
    interface PathFromToNotSupported extends SkipWithCosmosDB {
    }

    /**
     * min() and max() on not existing values causes "is not a valid number" exception
     *
     * @see SpecificsTest#minMaxBugs()
     */
    interface MinMaxBugs extends SkipWithCosmosDB {
    }

    /**
     * No <a href="http://tinkerpop.apache.org/docs/current/reference/#math-step">Math</a> Step
     *
     * @see SpecificsTest#noMath()
     */
    interface NoMath extends SkipWithCosmosDB {
    }

    /**
     * No <a href="http://tinkerpop.apache.org/docs/current/reference/#choose-step">none</a> token
     *
     * @see SpecificsTest#noNoneToken()
     */
    interface NoNoneToken extends SkipWithCosmosDB {
    }

    /**
     * Response chunks truncated to 4096 bytes on Azure server side.
     * Large responses are unparseable because of invalid JSON
     *
     * @see SpecificsTest#return100Elements()
     */
    interface Truncate4096 extends SkipWithCosmosDB {
    }

    /**
     * Traversal in property not supported
     *
     * @see SpecificsTest#traversalInProperty()
     */
    interface TraversalInProperty extends SkipWithCosmosDB {
    }

    /**
     * No <a href="http://tinkerpop.apache.org/docs/current/reference/#loops-step">Loops</a> Step which is required for
     * range implementation. Workaround is applied in rewriter {@link CosmosDbFlavor#rewriteRange}, which doesn't cover
     * all cases.
     *
     * @see CosmosDbFlavor#rewriteRange
     * @see SpecificsTest#loopsStep()
     */
    interface LoopsStepNotSupported extends SkipWithCosmosDB {
    }

    /**
     * Inner traversals are not <a href="https://github.com/Azure/azure-documentdb-dotnet/issues/316">supported</a>
     *
     * @see SpecificsTest#innerTraversals()
     */
    interface InnerTraversals extends SkipWithCosmosDB {
    }

    /**
     * Inconsistent behaviour on realiasing items
     *
     * @see SpecificsTest#realiasingCreatesCollection()
     */
    interface RealiasingCreatesCollection extends SkipWithCosmosDB {
    }

    /**
     * Inconsistent behaviour of <a href="http://tinkerpop.apache.org/docs/current/reference/#choose-step">choose step</a>
     *
     * @see SpecificsTest#choose()
     */
    interface Choose extends SkipWithCosmosDB {
    }

    /**
     * Range with negative values has different behaviour with reference implementation
     *
     *  @see SpecificsTest#negativeRange()
     */
    interface NegativeRange extends SkipWithCosmosDB {
    }

    /**
     * Sign is lost on negative values
     *
     *  @see SpecificsTest#signIsLost() ()
     */
    interface SignIsLost extends SkipWithCosmosDB {
    }

    /**
     * `[:]` is not recognized as empty map
     *
     * @see LiteralTest#returnEmptyMap()
     */
    interface EmptyMap extends SkipWithCosmosDB {
    }

    /**
     * `neq` predicate does not work on different types
     *
     * @see SpecificsTest#neqOnDifferentTypes()
     * @see CosmosDbFlavor#neqOnDiff
     */
    interface IsNeqOnDifferentTypes extends SkipWithCosmosDB {
    }

    /**
     * Inconsistent behaviour of <a href="http://tinkerpop.apache.org/docs/current/reference/#_values_step">values step</a>
     *
     * @see SpecificsTest#setAndGetProperty()
     */
    interface ValuesDoesNotWorkInSomeCases extends SkipWithCosmosDB {
    }

    /**
     * Cypher for Gremlin uses workaround {@link CustomFunctionFallback} to
     * <a href="https://stackoverflow.com/questions/53734954/how-can-i-return-meaningful-errors-in-gremlin">throw error in Gremlin</a>
     * Workaround is not applicable in Cosmos DB
     */
    interface NoKnownWayToThrowRuntimeException extends SkipWithCosmosDB {

    }
}
