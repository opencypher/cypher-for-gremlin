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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opencypher.gremlin.translation.Tokens;

public final class ParameterNormalizer {
    private ParameterNormalizer() {
    }

    public static Map<String, Object> normalize(Map<String, ?> parameters) {
        return normalizeMap(parameters);
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Map) {
            return normalizeMap((Map<?, ?>) value);
        } else if (value instanceof List) {
            return normalizeList((List<?>) value);
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        } else if (value == null) {
            return Tokens.NULL;
        }
        return value;
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> map) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            String key = String.valueOf(e.getKey());
            Object value = normalizeValue(e.getValue());
            result.put(key, value);
        }
        return result;
    }

    private static List<Object> normalizeList(List<?> list) {
        return list.stream()
            .map(ParameterNormalizer::normalizeValue)
            .collect(Collectors.toList());
    }
}
