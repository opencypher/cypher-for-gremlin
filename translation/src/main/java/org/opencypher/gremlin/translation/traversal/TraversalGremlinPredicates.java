/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.gremlin.translation.traversal;

import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.opencypher.gremlin.translation.GremlinPredicates;
import org.opencypher.gremlin.traversal.CustomPredicate;

public class TraversalGremlinPredicates implements GremlinPredicates<P> {

    @Override
    public P isEq(Object value) {
        return P.eq(value);
    }

    @Override
    public P gt(Object value) {
        return P.gt(value);
    }

    @Override
    public P gte(Object value) {
        return P.gte(value);
    }

    @Override
    public P lt(Object value) {
        return P.lt(value);
    }

    @Override
    public P lte(Object value) {
        return P.lte(value);
    }

    @Override
    public P neq(Object value) {
        return P.neq(value);
    }

    @Override
    public P between(Object first, Object second) {
        return P.between(first, second);
    }

    @Override
    public P within(Object... values) {
        return P.within(values);
    }

    @Override
    public P without(Object... values) {
        return P.without(values);
    }

    @Override
    public P startsWith(Object value) {
        return TextP.startingWith(value.toString());
    }

    @Override
    public P endsWith(Object value) {
        return TextP.endingWith(value.toString());
    }

    @Override
    public P contains(Object value) {
        return TextP.containing(value.toString());
    }

    @Override
    public P isNode() {
        return CustomPredicate.cypherIsNode();
    }

    @Override
    public P isRelationship() {
        return CustomPredicate.cypherIsRelationship();
    }

    @Override
    public P isString() {
        return CustomPredicate.cypherIsString();
    }
}
