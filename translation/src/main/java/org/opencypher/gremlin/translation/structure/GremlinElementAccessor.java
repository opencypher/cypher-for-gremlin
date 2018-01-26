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
import org.apache.tinkerpop.gremlin.structure.Property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class GremlinElementAccessor implements ElementAccessor {

    private final Element element;

    public GremlinElementAccessor(Element element) {
        this.element = element;
    }

    @Override
    public Set<String> keys() {
        return new HashSet<>(element.keys());
    }

    @Override
    public String label() {
        return element.label();
    }

    @Override
    public Object property(String name) {
        try {
            Property<Object> property = element.property(name);
            return property.isPresent() ? property.value() : null;
        } catch (IllegalStateException e) {
            Iterator<? extends Property<Object>> properties = element.properties(name);
            List<Object> result = new ArrayList<>();
            while (properties.hasNext()) {
                result.add(properties.next().value());
            }
            return result;
        }
    }
}
