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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.CypherType;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.NodeType;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.PathType;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.RelationshipType;
import org.opencypher.gremlin.translation.ReturnProperties;
import org.opencypher.gremlin.translation.Tokens;

public final class ReturnNormalizer {
    private final Map<String, CypherType> variableTypes;

    private ReturnNormalizer(Map<String, CypherType> variableTypes) {
        this.variableTypes = variableTypes;
    }

    public static ReturnNormalizer create(Map<String, CypherType> variableTypes) {
        return new ReturnNormalizer(variableTypes);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> normalize(Object row) {
        return (Map<String, Object>) normalizeValue(row);
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Map) {
            return normalizeMap((Map<?, ?>) value);
        } else if (value instanceof Collection) {
            return normalizeCollection((Collection<?>) value);
        } else if (value instanceof DetachedVertexProperty) {
            return elementPropertyMap((DetachedVertexProperty) value);
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        } else if (Tokens.NULL.equals(value)) {
            return null;
        } else if (value instanceof Traverser) {
            return normalize(((Traverser) value).get());
        }
        return value;
    }

    private Object normalizeValue(CypherType type, Object value) {
        if (Tokens.NULL.equals(value)) {
            return null;
        } else if (type instanceof NodeType) {
            return normalizeElement((Map<?, ?>) value, ReturnProperties.NODE_TYPE);
        } else if (type instanceof RelationshipType) {
            return normalizeRelationship((Map<?, ?>) value);
        } else if (type instanceof PathType) {
            return normalizePath((List<?>) value);
        }

        return normalizeValue(value);
    }

    private Map<Object, Object> normalizeElement(Map<?, ?> value, String type) {
        HashMap<Object, Object> result = new HashMap<>();
        result.put(ReturnProperties.TYPE, type);
        result.put(ReturnProperties.ID, value.get(T.id));
        result.put(ReturnProperties.LABEL, value.get(T.label));
        value.forEach(
            (k, v) -> {
                if (k instanceof String) {
                    if ((v instanceof Collection) && ((Collection) v).size() == 1) {
                        result.put(k, normalizeValue(((Collection) v).iterator().next()));
                    } else {
                        result.put(k, normalizeValue(v));
                    }
                }
            });

        return result;
    }

    private Map<Object, Object> normalizeRelationship(Map<?, ?> value) {
        HashMap<Object, Object> result = new HashMap<>();
        result.put(ReturnProperties.TYPE, ReturnProperties.RELATIONSHIP_TYPE);
        result.put(ReturnProperties.INV, value.get(Tokens.PROJECTION_INV));
        result.put(ReturnProperties.OUTV, value.get(Tokens.PROJECTION_OUTV));

        if (value.containsKey(Tokens.ELEMENT)) {
            Map<?, ?> element = (Map<?, ?>) value.get(Tokens.ELEMENT);

            result.put(ReturnProperties.ID, element.remove(T.id));
            result.put(ReturnProperties.LABEL, element.remove(T.label));

            element.forEach(
                (k, v) -> result.put(String.valueOf(k), normalizeValue(v)));
        }

        return result;
    }

    private Object normalizePath(List<?> value) {
        return value;
    }

    private Map<?, ?> normalizeMap(Map<?, ?> map) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            Object value = variableTypes.containsKey(key) ?
                normalizeValue(variableTypes.get(key), e.getValue()) :
                normalizeValue(e.getValue());

            result.put(key, value);
        }
        return result;
    }

    private Collection<?> normalizeCollection(Collection<?> value) {
        return value.stream()
            .map(this::normalizeValue)
            .collect(Collectors.toList());
    }

    private Object elementPropertyMap(Element element) {
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        Iterator<? extends Property<Object>> properties = element.properties();
        while (properties.hasNext()) {
            Property<Object> next = properties.next();
            result.put(next.key(), next.value());
        }
        return result;
    }
}
