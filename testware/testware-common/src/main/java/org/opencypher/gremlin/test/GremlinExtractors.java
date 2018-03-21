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
package org.opencypher.gremlin.test;

import static org.assertj.core.groups.Tuple.tuple;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.groups.Tuple;

public final class GremlinExtractors {

    @SuppressWarnings("unchecked")
    public static <F> Extractor<F, Object> byElementProperty(String propertyName) {
        return element -> propertyValue((Map<String, Object>) element, propertyName);
    }

    @SuppressWarnings("unchecked")
    public static <F> Extractor<F, Tuple> byElementProperty(String... propertyNames) {
        return element -> {
            Object[] values = Stream.of(propertyNames)
                .map(name -> propertyValue((Map<String, Object>) element, name))
                .toArray();
            return tuple(values);
        };
    }

    private static Object propertyValue(Map<String, Object> element, String propertyName) {
        Object value = element.get(propertyName);
        if (value instanceof Collection) {
            Collection values = (Collection) value;
            if (values.isEmpty()){
                return null;
            }
        }
        return value;
    }

}
