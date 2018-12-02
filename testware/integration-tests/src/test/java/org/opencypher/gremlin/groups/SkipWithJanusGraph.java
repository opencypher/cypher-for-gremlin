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
 * Tests that are skipped because of JanusGraph specifics. Refer to categories for more details.
 */
public interface SkipWithJanusGraph {
    /**
     * Because of <a href="https://docs.janusgraph.org/latest/schema.html#_automatic_schema_maker">schema</a>,
     * after first assignment, property type can not be changed with Gremlin traversal.
     *
     * @see SpecificsTest#changePropertyType()
     * @see SpecificsTest#changePropertyType2()
     */
    interface ChangePropertyType extends SkipWithJanusGraph {
    }

    /**
     * Unable to get edge property just after setting it.
     *
     * @see SpecificsTest#setAndGetEdgeProperty()
     */
    interface SetAndGetEdgeProperty extends SkipWithJanusGraph {
    }

    /**
     * In rare cases, JanusGraph does not return {@link Throwable#detailMessage}.
     */
    interface NoExceptionDetailMessage extends SkipWithJanusGraph {
    }
}
