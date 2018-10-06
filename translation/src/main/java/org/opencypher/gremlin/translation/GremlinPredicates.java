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

import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.CustomPredicate;

/**
 * Gremlin {@link org.apache.tinkerpop.gremlin.process.traversal.P} predicate abstraction.
 * For DSL details, see
 * <a href="https://tinkerpop.apache.org/docs/current/reference/#a-note-on-predicates">A Note on Predicates</a>.
 *
 * @param <P> translation predicate type
 * @see CustomPredicate
 * @see Translator
 */
public interface GremlinPredicates<P> {
    P isEq(Object value); // Named like this to satisfy Scala compiler

    P gt(Object value);

    P gte(Object value);

    P lt(Object value);

    P lte(Object value);

    P neq(Object value);

    P between(Object first, Object second);

    P within(Object... values);

    P without(Object... values);

    P startsWith(Object value);

    P endsWith(Object value);

    P contains(Object value);

    P isNode();

    P isString();
}
