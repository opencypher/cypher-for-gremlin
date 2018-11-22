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
}
