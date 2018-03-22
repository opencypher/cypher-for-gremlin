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
import java.util.Arrays;
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
import org.opencypher.gremlin.translation.Tokens;

public final class ReturnNormalizer {
    public static final String ID = " cypher.id";
    public static final String LABEL = " cypher.label";
    public static final String TYPE = " cypher.type";
    public static final String ELEMENT = " cypher.element";
    public static final String INV = " cypher.inv";
    public static final String OUTV = " cypher.outv";

    public static final List<String> VALUES = Arrays.asList(ID, LABEL, TYPE, ELEMENT, INV, OUTV);

    public static final String NODE = "node";
    public static final String RELATIONSHIP = "relationship";


    private Map<String, CypherType> variableTypes;

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

    private Object normalizeValue(CypherType type, Object value) {
        if (Tokens.NULL.equals(value)) {
            return null;
        } else if (type instanceof NodeType) {
            return normalizeElement((Map<?, ?>) value, NODE);
        } else if (type instanceof RelationshipType) {
            return normalizeRelationship((Map<?, ?>) value);
        } else if (type instanceof PathType) {
            return normalizePath((List<?>) value);
        }

        return normalizeValue(value);
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

    private Map<Object, Object> normalizeElement(Map<?, ?> value, String type) {
        HashMap<Object, Object> result = new HashMap<>();
        result.put(TYPE, type);
        result.put(ID, value.get(T.id));
        result.put(LABEL, value.get(T.label));
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
        result.put(TYPE, RELATIONSHIP);
        result.put(INV, value.get(INV));
        result.put(OUTV, value.get(OUTV));

        if (value.containsKey(ELEMENT)) {
            Map<?, ?> element = (Map<?, ?>) value.get(ELEMENT);

            result.put(ID, element.remove(T.id));
            result.put(LABEL, element.remove(T.label));

            element.forEach(
                (k, v) -> result.put(String.valueOf(k), normalizeValue(v)));
        }

        return result;
    }

    private Object normalizePath(List<?> value) {
        boolean isNode = true;
        Map<Object, Object> prevNode = null;
        Map<Object, Object> prevRelationship = new HashMap<>();

        List<Object> result = new ArrayList<>();
        for (Object e : value) {
            if (isNode) {
                prevNode = normalizeElement((Map<?, ?>) e, NODE);
                result.add(prevNode);
                prevRelationship.put(OUTV, prevNode.get(ID));
            } else {
                prevRelationship = normalizeElement((Map<?, ?>) e, RELATIONSHIP);
                prevRelationship.put(INV, prevNode.get(ID));
                result.add(prevRelationship);
            }
            isNode = !isNode;
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
