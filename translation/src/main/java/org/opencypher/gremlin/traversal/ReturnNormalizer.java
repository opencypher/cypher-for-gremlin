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
package org.opencypher.gremlin.traversal;

import static org.opencypher.gremlin.translation.ReturnProperties.ID;
import static org.opencypher.gremlin.translation.ReturnProperties.INV;
import static org.opencypher.gremlin.translation.ReturnProperties.LABEL;
import static org.opencypher.gremlin.translation.ReturnProperties.NODE_TYPE;
import static org.opencypher.gremlin.translation.ReturnProperties.OUTV;
import static org.opencypher.gremlin.translation.ReturnProperties.RELATIONSHIP_TYPE;
import static org.opencypher.gremlin.translation.ReturnProperties.TYPE;
import static org.opencypher.gremlin.translation.Tokens.PROJECTION_ELEMENT;
import static org.opencypher.gremlin.translation.Tokens.PROJECTION_ID;
import static org.opencypher.gremlin.translation.Tokens.PROJECTION_INV;
import static org.opencypher.gremlin.translation.Tokens.PROJECTION_OUTV;
import static org.opencypher.gremlin.translation.Tokens.PROJECTION_RELATIONSHIP;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.opencypher.gremlin.translation.Tokens;
import org.opencypher.v9_0.util.symbols.CypherType;
import org.opencypher.v9_0.util.symbols.IntegerType;
import org.opencypher.v9_0.util.symbols.ListType;
import org.opencypher.v9_0.util.symbols.NodeType;
import org.opencypher.v9_0.util.symbols.PathType;
import org.opencypher.v9_0.util.symbols.RelationshipType;

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
        if (row instanceof String) {
            throw new IllegalStateException("Invalid response: expected Map, got String." +
                " Probable cause: 'serializeResultToString' set to 'true' in Gremlin serializer config");
        }

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
        } else if (value instanceof Double && Double.isNaN((double) value)) {
            return null;
        } else if (value instanceof Traverser) {
            return normalize(((Traverser) value).get());
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeValue(CypherType type, Object value) {
        if (Tokens.NULL.equals(value)) {
            return null;
        } else if (type instanceof NodeType) {
            return normalizeElement((Map<?, ?>) value, NODE_TYPE);
        } else if (type instanceof RelationshipType) {
            return normalizeRelationship((Map<?, ?>) value);
        } else if (type instanceof PathType) {
            return normalizePath((Map<?, ?>) value);
        } else if (type instanceof IntegerType) {
            return normalizeInteger(value);
        } else if (type instanceof ListType) {
            CypherType cypherType = ((ListType) type).innerType();
            return ((Collection<?>) value)
                .stream()
                .map(v -> normalizeValue(cypherType, v))
                .collect(Collectors.toList());
        }

        return normalizeValue(value);
    }

    private Map<Object, Object> normalizeElement(Map<?, ?> value, String type) {
        HashMap<Object, Object> result = new HashMap<>();
        result.put(TYPE, type);
        result.put(ID, getT(value, T.id));
        result.put(LABEL, getT(value, T.label));

        boolean gremlinTokensCanBeMapKeys = value.containsKey(T.id);
        value.entrySet().stream()
            .filter(e1 -> isProperty(e1, gremlinTokensCanBeMapKeys))
            .forEach(
                e -> {
                    if (NODE_TYPE.equals(type) && isVertexValueList(e.getValue())) {
                        result.put(e.getKey(), normalizeValue(((Collection) e.getValue()).iterator().next()));
                    } else {
                        result.put(e.getKey(), normalizeValue(e.getValue()));
                    }
                });

        return result;
    }

    private Map<Object, Object> normalizeRelationship(Map<?, ?> value) {
        HashMap<Object, Object> result = new HashMap<>();
        result.put(TYPE, RELATIONSHIP_TYPE);
        result.put(INV, value.get(PROJECTION_INV));
        result.put(OUTV, value.get(PROJECTION_OUTV));

        if (value.containsKey(PROJECTION_ELEMENT)) {
            Map<?, ?> element = (Map<?, ?>) value.get(PROJECTION_ELEMENT);

            result.put(ID, getT(element, T.id));
            result.put(LABEL, getT(element, T.label));

            boolean gremlinTokensCanBeMapKeys = element.containsKey(T.id);
            element.entrySet().stream()
                .filter(e -> isProperty(e, gremlinTokensCanBeMapKeys))
                .forEach(e -> result.put(e.getKey(), normalizeValue(e.getValue())));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private Object normalizePath(Map<?, ?> value) {
        List<Map<?, ?>> relationships = (List<Map<?, ?>>) value.get(PROJECTION_RELATIONSHIP);
        List<Map<?, ?>> elements = (List<Map<?, ?>>) value.get(PROJECTION_ELEMENT);

        HashMap<Object, Map<?, ?>> relationshipMap = new HashMap<>();
        for (Map<?, ?> relationship : relationships) {
            relationshipMap.put(relationship.get(PROJECTION_ID), relationship);
        }

        List<Object> result = new ArrayList<>();
        for (Map<?, ?> element : elements) {
            Object id = getT(element, T.id);
            boolean isRelationship = relationshipMap.containsKey(id);

            Map<Object, Object> normalized = normalizeElement(element, isRelationship ? RELATIONSHIP_TYPE : NODE_TYPE);
            if (isRelationship) {
                normalized.put(INV, relationshipMap.get(id).get(PROJECTION_INV));
                normalized.put(OUTV, relationshipMap.get(id).get(PROJECTION_OUTV));
            }

            result.add(normalized);
        }

        return result;
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

    Collection<?> normalizeCollection(Collection<?> value) {
        return value.stream()
            .map(this::normalizeValue)
            .collect(Collectors.toList());
    }

    private Object normalizeInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return value;
        }
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

    private Object getT(Map<?, ?> element, T prop) {
        return element.containsKey(prop) ?
            element.get(prop) :
            element.get(prop.toString());
    }

    private boolean isVertexValueList(Object e) {
        return (e instanceof Collection) && ((Collection) e).size() == 1;
    }

    private boolean isProperty(Entry<?, ?> e, boolean gremlinTokensCanBeMapKeys) {
        if (gremlinTokensCanBeMapKeys) {
            return !T.id.equals(e.getKey()) &&
                !T.label.equals(e.getKey());
        } else {
            return !("label".equals(e.getKey()) && !isVertexValueList(e.getValue())) &&
                !("id".equals(e.getKey()) && !isVertexValueList(e.getValue()));
        }
    }
}
