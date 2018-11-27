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
     */
    interface Truncate4096 extends SkipWithCosmosDB {
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
      * Combination of `group()` and `choose()` behavior is different from reference implementation. Is it supported?
      *  - Query `g.V().as('n').select('n').group().by(choose(hasLabel('person'), constant(true), constant(false))).by("name")` on [Modern Graph](https://tinkerpop.apache.org/docs/current/reference/#intro):
      *  - TinkerGraph returns: `[false:[lop,ripple],true:[josh,peter,marko,vadas]]`
      *  - Cosmos DB returns: `[true:[marko,vadas,lop,josh,ripple,peter]]`
     */
    interface GroupChoose extends SkipWithCosmosDB {
    }

    /**
     * g.inject(1).is(neq('a'))
     */
    interface IsNeqOnDifferentTypes extends SkipWithCosmosDB {
    }


}
