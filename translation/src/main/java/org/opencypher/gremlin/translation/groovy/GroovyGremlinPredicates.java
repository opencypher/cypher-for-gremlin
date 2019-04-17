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
package org.opencypher.gremlin.translation.groovy;

import org.opencypher.gremlin.translation.GremlinPredicates;

public class GroovyGremlinPredicates implements GremlinPredicates<GroovyPredicate> {

    public GroovyPredicate eq(Object value) {
        return isEq(value);
    }

    @Override
    public GroovyPredicate isEq(Object value) {
        return new GroovyPredicate("eq", value);
    }

    @Override
    public GroovyPredicate gt(Object value) {
        return new GroovyPredicate("gt", value);
    }

    @Override
    public GroovyPredicate gte(Object value) {
        return new GroovyPredicate("gte", value);
    }

    @Override
    public GroovyPredicate lt(Object value) {
        return new GroovyPredicate("lt", value);
    }

    @Override
    public GroovyPredicate lte(Object value) {
        return new GroovyPredicate("lte", value);
    }

    @Override
    public GroovyPredicate neq(Object value) {
        return new GroovyPredicate("neq", value);
    }

    @Override
    public GroovyPredicate between(Object first, Object second) {
        return new GroovyPredicate("between", first, second);
    }

    @Override
    public GroovyPredicate within(Object... values) {
        return new GroovyPredicate("within", values);
    }

    @Override
    public GroovyPredicate without(Object... values) {
        return new GroovyPredicate("without", values);
    }

    @Override
    public GroovyPredicate startsWith(Object value) {
        return new GroovyPredicate("startingWith", value);
    }

    @Override
    public GroovyPredicate endsWith(Object value) {
        return new GroovyPredicate("endingWith", value);
    }

    @Override
    public GroovyPredicate contains(Object value) {
        return new GroovyPredicate("containing", value);
    }

    @Override
    public GroovyPredicate regexMatch(Object value) {
        return new GroovyPredicate("cypherRegex", value);
    }

    @Override
    public GroovyPredicate isNode() {
        return new GroovyPredicate("cypherIsNode");
    }

    @Override
    public GroovyPredicate isRelationship() {
        return new GroovyPredicate("cypherIsRelationship");
    }

    @Override
    public GroovyPredicate isString() {
        return new GroovyPredicate("cypherIsString");
    }
}
