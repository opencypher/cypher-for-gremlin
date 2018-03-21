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
package org.opencypher;

import static java.lang.String.format;
import static org.opencypher.gremlin.traversal.ReturnNormalizer.NODE;
import static org.opencypher.gremlin.traversal.ReturnNormalizer.RELATIONSHIP;
import static org.opencypher.gremlin.traversal.ReturnNormalizer.TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.InternalRecord;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Entity;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

class GremlinCypherValueConverter {

    static Record toRecord(Map<String, Object> map) {
        List<String> keys = new ArrayList<>();
        List<Value> values = new ArrayList<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            keys.add(entry.getKey());
            values.add(toCypherValue(entry.getValue()));
        }

        return new InternalRecord(keys, values.toArray(new Value[values.size()]));
    }

    private static Value toCypherValue(Object value) {
        if (isNode(value)) {
            return toCypherNode((Map<?, ?>) value).asValue();
        } else if (isRelationship(value)) {
            return toCypherRelationship((Map<?, ?>) value).asValue();
        } else if (isPath(value)) {
            return toCypherPath((List) value);
        } else {
            return Values.value(value);
        }
    }

    private static boolean isNode(Object value) {
        return ((value instanceof Map) && NODE.equals(((Map) value).get(TYPE)));
    }

    private static boolean isRelationship(Object value) {
        return ((value instanceof Map) && RELATIONSHIP.equals(((Map) value).get(TYPE)));
    }

    private static boolean isPath(Object value) {
        if (!(value instanceof List)) {
            return false;
        }

        for (Object e : (List) value) {
            if (!isNode(e) && !isRelationship(e)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static Value toCypherPath(List p) {
        boolean isNode = true;

        Entity[] objects = new Entity[p.size()];
        for (int i = 0; i < p.size(); i++) {
            if (isNode) {
                objects[i] = toCypherNode((Map<?, ?>) p.get(i));
            } else {
                objects[i] = toCypherRelationship((Map<?, ?>) p.get(i));
            }

            isNode = !isNode;
        }

        return new InternalPath(objects).asValue();
    }

    private static InternalRelationship toCypherRelationship(Map<?, ?> e) {
        Long start = toCypherId(e.get(ReturnNormalizer.OUTV));
        Long end = toCypherId(e.get(ReturnNormalizer.INV));

        Map<String, Value> properties = toCypherPropertyMap(e);

        Object id = e.get(ReturnNormalizer.ID);
        Object label = e.get(ReturnNormalizer.LABEL);

        return new InternalRelationship(toCypherId(id), start, end, String.valueOf(label), properties);
    }

    private static InternalNode toCypherNode(Map<?, ?> v) {
        Set<String> labels = new HashSet<>();
        String label = String.valueOf(v.get(ReturnNormalizer.LABEL));
        if (!Vertex.DEFAULT_LABEL.equals(label)) {
            labels.add(label);
        }

        Map<String, Value> properties = toCypherPropertyMap(v);

        return new InternalNode(toCypherId(v.get(ReturnNormalizer.ID)), labels, properties);
    }

    private static Map<String, Value> toCypherPropertyMap(Map<?, ?> e) {
        Map<String, Value> properties = new HashMap<>();
        e.forEach((key, value) -> properties.put(
            String.valueOf(key),
            toCypherValue(value)));
        properties.remove(ReturnNormalizer.ID);
        properties.remove(ReturnNormalizer.LABEL);

        return properties;
    }

    private static Long toCypherId(Object id) {
        try {
            return Long.valueOf(String.valueOf(id));
        } catch (Exception e) {
            throw new IllegalArgumentException(format("Entity id should be numeric, got `%s` (%s):", id.getClass(), id));
        }
    }
}
