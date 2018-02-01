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

import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;
import org.opencypher.gremlin.translation.Tokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReturnNormalizer {

    @SuppressWarnings("unchecked")
    public static <S> Function<Traverser<S>, Map<String, Object>> toCypherResults() {
        return traverser -> {
            Map maps = (Map) traverser.get();
            return normalize(maps);
        };
    }

    // https://bugs.openjdk.java.net/browse/JDK-8148463
    public static Map<String, Object> normalize(Map<String, ?> row) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();

        for (Entry<String, ?> e : row.entrySet()) {
            result.put(e.getKey(), normalize(e.getValue()));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    static Object normalize(Object value) {
        if (value instanceof Map) {
            return normalize(Map.class.cast(value));
        } else if (value instanceof Collection) {
            return normalize(Collection.class.cast(value));
        } else if (value instanceof DetachedVertexProperty) {
            return extractMap(DetachedVertexProperty.class.cast(value));
        } else if (value instanceof Integer) {
            return Integer.class.cast(value).longValue();
        } else if (Tokens.NULL.equals(value)) {
            return null;
        } else if (value instanceof Path) {
            return new ArrayList<>(((Path) value).objects());
        }
        return value;
    }

    private static Object extractMap(Element element) {
        LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
        Iterator<? extends Property<Object>> properties = element.properties();
        while (properties.hasNext()) {
            Property<Object> next = properties.next();
            result.put(next.key(), next.value());
        }

        return result;
    }


    static Object normalize(Collection<Object> collection) {
        return collection
            .stream()
            .map(ReturnNormalizer::normalize)
            .collect(Collectors.toList());
    }
}
