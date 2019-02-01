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

import org.opencypher.gremlin.queries.SpecificsTest;

/**
 * Tests that are skipped because of Cosmos DB specifics. Refer to categories for more details.
 */
public interface SkipWithCosmosDB {
    /**
     */
    interface PathFromToNotSupported extends SkipWithCosmosDB {
    }

    /**
     */
    interface MinMaxBugs extends SkipWithCosmosDB {
    }

    /**
     */
    interface NoMath extends SkipWithCosmosDB {
    }

    /**
     * by(__.choose(__.identity()).
     *         option(13, __.constant('integer')).
     *         option(3.14d, __.constant('float')).
     *         option('bingo', __.constant('string')).
     *         option(true, __.constant('boolean')).
     *         option('  cypher.null', __.constant('null')).
     *         option(['a'], __.constant('collection')).
     *         option(none, __.constant('  cypher.null')))
     */
    interface NoNoneToken extends SkipWithCosmosDB {
    }

    /**
     */
    interface Truncate4096 extends SkipWithCosmosDB {
    }

    /**
     */
    interface MaxRequest extends SkipWithCosmosDB {
    }

    /**
     * g.inject(1).as('i').addV().property('value', select('i'))
     */
    interface TraversalInProperty extends SkipWithCosmosDB {
    }

    /**
     */
    interface RangeWithExpression extends SkipWithCosmosDB {
    }

    /**
     * Inner traversals are not supported
     * https://github.com/Azure/azure-documentdb-dotnet/issues/316
     */
    interface InnerTraversals extends SkipWithCosmosDB {
    }

    /**
     - `g.inject(1).as('x').select('x')` returns `1`
     - `g.inject(1).as('x').select('x').as('x').select('x')` returns `[1,1]`
     - `g.inject(1).as('x').select('x').as('x').select('x').as('x').select('x')` returns `[1,1,[1,1]]`
     */
    interface RealiasingCreatesCollection extends SkipWithCosmosDB {
    }

    /**
     *  @see SpecificsTest#choose()
     */
    interface Choose extends SkipWithCosmosDB {
    }

    /**
     * g.inject(1).is(neq('a'))
     */
    interface IsNeqOnDifferentTypes extends SkipWithCosmosDB {
    }

    interface ValuesDoesNotWorkInSomeCases extends SkipWithCosmosDB {

    }

    /**
     * https://stackoverflow.com/questions/53734954/how-can-i-return-meaningful-errors-in-gremlin
     */
    interface NoKnownWayToThrowRuntimeException extends SkipWithCosmosDB {

    }


}
