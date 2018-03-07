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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.internal.InternalRecord;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Entity;

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
        if (value instanceof Vertex) {
            return toCypherVertex((Vertex) value).asValue();
        } else if (value instanceof Edge) {
            return toCypherEdge((Edge) value).asValue();
        } else if (isPath(value)) {
            return toCypherPath((List) value);
        } else {
            return Values.value(value);
        }
    }

    private static Entity toCypherElement(Object value) {
        if (value instanceof Vertex) {
            return toCypherVertex((Vertex) value);
        } else if (value instanceof Edge) {
            return toCypherEdge((Edge) value);
        } else {
            throw new IllegalArgumentException(value + "is not path");
        }
    }

    private static boolean isPath(Object value) {
        if (!(value instanceof List)) {
            return false;
        }

        for (Object e : (List) value) {
            if (!(e instanceof Vertex || e instanceof Edge)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    private static Value toCypherPath(List p) {
        Entity[] objects = ((List<Object>) p).stream()
            .map(GremlinCypherValueConverter::toCypherElement)
            .toArray(Entity[]::new);

        return new InternalPath(objects).asValue();
    }

    private static InternalRelationship toCypherEdge(Edge e) {
        Long start = toCypherId(e.outVertex().id());
        Long end = toCypherId(e.inVertex().id());

        return new InternalRelationship(toCypherId(e.id()), start, end, e.label());
    }

    private static InternalNode toCypherVertex(Vertex v) {
        Set<String> labels = new HashSet<>();
        if (!Vertex.DEFAULT_LABEL.equals(v.label())) {
            labels.add(v.label());
        }

        Map<String, Value> properties = toCypherPropertyMap(v);

        return new InternalNode(toCypherId(v.id()), labels, properties);
    }

    private static Map<String, Value> toCypherPropertyMap(Element e) {
        Map<String, Value> properties = new HashMap<>();
        e.properties().forEachRemaining(p -> properties.put(p.key(), toCypherValue(p.value())));
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
