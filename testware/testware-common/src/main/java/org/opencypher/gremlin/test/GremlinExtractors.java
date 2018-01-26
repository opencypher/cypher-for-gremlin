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

import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.groups.Tuple;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.groups.Tuple.tuple;

public final class GremlinExtractors {

    public static <F> Extractor<F, Object> byElementProperty(String propertyName) {
        return element -> propertyValue((Element) element, propertyName);
    }

    public static <F> Extractor<F, Tuple> byElementProperty(String... propertyNames) {
        return element -> {
            Object[] values = Stream.of(propertyNames)
                .map(name -> propertyValue((Element) element, name))
                .toArray();
            return tuple(values);
        };
    }

    private static Object propertyValue(Element element, String propertyName) {
        Iterator<? extends Property<Object>> properties = element.properties(propertyName);
        List<Object> values = stream(properties)
            .map(property -> property.orElse(null))
            .collect(toList());
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        return values;
    }

}
