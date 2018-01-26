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
package org.opencypher.gremlin.translation;

public interface PredicateFactory<T> {
    T isEq(Object value); // Named like this to satisfy Scala compiler

    T gt(Object value);

    T gte(Object value);

    T lt(Object value);

    T lte(Object value);

    T neq(Object value);

    T between(Object first, Object second);

    T within(Object... values);

    T without(Object... values);

    T startsWith(Object value);

    T endsWith(Object value);

    T contains(Object value);
}
