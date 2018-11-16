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

import org.opencypher.gremlin.traversal.CustomPredicate;

/**
 * Tests that use custom functions or predicates. Requires <a href="https://github.com/opencypher/cypher-for-gremlin/tree/master/tinkerpop/cypher-gremlin-server-plugin">Gremlin Server Cypher Plugin</a>
 * or <a href="https://github.com/opencypher/cypher-for-gremlin/tree/master/tinkerpop/cypher-gremlin-extensions">Gremlin Cypher Extensions</a>
 * to be installed on target.
 */
public interface UsesExtensions {
    /**
     * @see CustomFunctions
     */
    interface CustomFunctions extends UsesExtensions {
    }

    /**
     * @see CustomPredicate
     */
    interface CustomPredicates extends UsesExtensions {
    }
}
