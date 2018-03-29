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
package org.opencypher.gremlin.translation.groovy;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;

public final class StringTranslationUtils {
    private StringTranslationUtils() {
    }

    public static String apply(String name, Object... arguments) {
        String joined = Stream.of(arguments)
            .map(StringTranslationUtils::toLiteral)
            .collect(joining(", "));
        return name + "(" + joined + ")";
    }

    static String chain(String name, Object... arguments) {
        return "." + apply(name, arguments);
    }

    public static String toLiteral(Object argument) {
        if (argument instanceof List) {
            return ((List<?>) argument).stream()
                .map(StringTranslationUtils::toLiteral)
                .collect(Collectors.joining(", ", "[", "]"));
        }
        if (argument instanceof DetachedVertexProperty) {
            Map<String, Object> map = new HashMap<>();
            Iterator<Property<Object>> properties = ((DetachedVertexProperty<?>) argument).properties();
            properties
                .forEachRemaining(e -> map.put(e.key(), e.value()));
            return toLiteral(map);
        }
        if (argument instanceof Map) {
            return ((Map<?, ?>) argument).entrySet().stream()
                .map(entry -> {
                    Object key = entry.getKey();
                    Object value = toLiteral(entry.getValue());
                    return key + ": " + value;
                })
                .collect(Collectors.joining(", ", "[", "]"));
        }
        if (argument instanceof String) {
            return toStringLiteral((String) argument);
        }
        if (argument == null) {
            return "null";
        }
        if (argument instanceof Verbatim) {
            return ((Verbatim) argument).getValue();
        }

        return argument.toString();
    }

    private static String toStringLiteral(String agrument) {
        return "'" + agrument.replaceAll("(['\\\\])", "\\\\$1") + "'";
    }

}
