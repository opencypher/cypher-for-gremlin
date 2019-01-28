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
package org.opencypher.gremlin.translation.bytecode;

import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode.Binding;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.TextP;
import org.opencypher.gremlin.translation.GremlinPredicates;
import org.opencypher.gremlin.traversal.CustomPredicate;

public class BytecodeGremlinPredicates implements GremlinPredicates<P> {

    @Override
    public P isEq(Object value) {
        return P.eq(inlineParameter(value));
    }

    @Override
    public P gt(Object value) {
        return P.gt(inlineParameter(value));
    }

    @Override
    public P gte(Object value) {
        return P.gte(inlineParameter(value));
    }

    @Override
    public P lt(Object value) {
        return P.lt(inlineParameter(value));
    }

    @Override
    public P lte(Object value) {
        return P.lte(inlineParameter(value));
    }

    @Override
    public P neq(Object value) {
        return P.neq(inlineParameter(value));
    }

    @Override
    public P between(Object first, Object second) {
        return P.between(inlineParameter(first), inlineParameter(second));
    }

    @Override
    public P within(Object... values) {
        return P.within(inlineParameters(values));
    }

    @Override
    public P without(Object... values) {
        return P.without(inlineParameters(values));
    }

    @Override
    public P startsWith(Object value) {
        return TextP.startingWith(inlineParameter(value).toString());
    }

    @Override
    public P endsWith(Object value) {
        return TextP.endingWith(inlineParameter(value).toString());
    }

    @Override
    public P contains(Object value) {
        return TextP.containing(inlineParameter(value).toString());
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

    private static Object[] inlineParameters(Object... values) {
        return Stream.of(values)
            .map(BytecodeGremlinPredicates::inlineParameter)
            .toArray();
    }

    private static Object inlineParameter(Object value) {
        if (value instanceof Binding) {
            return ((Binding) value).value();
        }
        return value;
    }
}
