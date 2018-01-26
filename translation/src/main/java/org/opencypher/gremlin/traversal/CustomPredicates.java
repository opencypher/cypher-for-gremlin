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

import org.apache.tinkerpop.gremlin.process.traversal.P;

import java.util.function.BiPredicate;

public class CustomPredicates {
    public static <V> P<V> startsWith(final V prefix) {
        return considerNull(prefix, String::startsWith);
    }

    public static <V> P<V> endsWith(final V suffix) {
        return considerNull(suffix, String::endsWith);
    }

    public static <V> P<V> contains(final V sequence) {
        return considerNull(sequence, String::contains);
    }

    /**
     * Cypher allows null as function argument
     */
    private static <V> P<V> considerNull(V value, BiPredicate<String, String> predicate) {
        return new P<>(
            (o, o2) -> o != null
                && o2 != null
                && predicate.test(o.toString(), o2.toString())
            , value);
    }
}
