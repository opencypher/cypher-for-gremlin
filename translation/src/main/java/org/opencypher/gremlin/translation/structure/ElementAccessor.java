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
package org.opencypher.gremlin.translation.structure;

import org.apache.tinkerpop.gremlin.structure.Element;
import org.opencypher.gremlin.translation.exception.TypeException;

import java.util.Map;
import java.util.Set;

public interface ElementAccessor {

    @SuppressWarnings("unchecked")
    static ElementAccessor of(Object object) {
        if (object == null) {
            return new NullElementAccessor();
        }
        if (object instanceof Element) {
            return new GremlinElementAccessor((Element) object);
        }
        if (object instanceof Map) {
            return new GraphSONElementAccessor((Map<String, Object>) object);
        }
        throw new TypeException("Unsupported graph element type: " + object.getClass());
    }

    Set<String> keys();

    String label();

    Object property(String name);
}
