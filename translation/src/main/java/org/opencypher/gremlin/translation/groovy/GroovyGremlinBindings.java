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
package org.opencypher.gremlin.translation.groovy;

import static org.opencypher.gremlin.translation.GroovyIdentifiers.isValidIdentifier;

import org.opencypher.gremlin.translation.GremlinBindings;

public class GroovyGremlinBindings implements GremlinBindings {
    @Override
    public Object bind(String name, Object value) {
        if (isValidIdentifier(name)) {
            return Verbatim.of(name);
        } else {
            throw new IllegalArgumentException("Invalid parameter name: " + name);
        }
    }
}
