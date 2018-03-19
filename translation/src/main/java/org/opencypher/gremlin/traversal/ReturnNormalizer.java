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
package org.opencypher.gremlin.traversal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.opencypher.gremlin.translation.Tokens;

public final class ReturnNormalizer {
    private ReturnNormalizer() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalize(Object row) {
        return (Map<String, Object>) normalizeValue(row);
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Map) {
            return normalizeMap((Map<?, ?>) value);
        } else if (value instanceof Collection) {
            return normalizeCollection((Collection<?>) value);
        } else if (value instanceof DetachedVertex) {
            return normalizeDetachedVertex((DetachedVertex) value);
        } else if (value instanceof DetachedVertexProperty) {
            return elementPropertyMap((DetachedVertexProperty) value);
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        } else if (Tokens.NULL.equals(value)) {
            return null;
        } else if (value instanceof Path) {
            return new ArrayList<>(((Path) value).objects());
        } else if (value instanceof Traverser) {
            return normalize(((Traverser) value).get());
        }
        return value;
    }

    private static Map<?, ?> normalizeMap(Map<?, ?> value) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Entry<?, ?> e : value.entrySet()) {
            result.put(String.valueOf(e.getKey()), normalizeValue(e.getValue()));
        }
        return result;
    }

    private static Collection<?> normalizeCollection(Collection<?> value) {
        return value.stream()
            .map(ReturnNormalizer::normalizeValue)
            .collect(Collectors.toList());
    }

    private static DetachedVertex normalizeDetachedVertex(DetachedVertex vertex) {
        DetachedVertex.Builder builder = DetachedVertex.build()
            .setId(vertex.id())
            .setLabel(vertex.label());
        Iterator<VertexProperty<Object>> properties = vertex.properties();
        while (properties.hasNext()) {
            VertexProperty<Object> property = properties.next();
            DetachedVertexProperty normalizedProperty = DetachedVertexProperty.build()
                .setId(property.id())
                .setLabel(property.label())
                .setV(vertex)
                .setValue(normalizeValue(property.value()))
                .create();
            builder.addProperty(normalizedProperty);
        }
        return builder.create();
    }

    private static Object elementPropertyMap(Element element) {
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        Iterator<? extends Property<Object>> properties = element.properties();
        while (properties.hasNext()) {
            Property<Object> next = properties.next();
            result.put(next.key(), next.value());
        }
        return result;
    }
}
