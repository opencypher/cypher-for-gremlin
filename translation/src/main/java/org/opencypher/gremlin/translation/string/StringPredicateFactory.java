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
package org.opencypher.gremlin.translation.string;

import org.opencypher.gremlin.translation.PredicateFactory;

public class StringPredicateFactory implements PredicateFactory<StringPredicate> {

    public StringPredicate eq(Object value) {
        return isEq(value);
    }

    @Override
    public StringPredicate isEq(Object value) {
        return new StringPredicate("eq", value);
    }

    @Override
    public StringPredicate gt(Object value) {
        return new StringPredicate("gt", value);
    }

    @Override
    public StringPredicate gte(Object value) {
        return new StringPredicate("gte", value);
    }

    @Override
    public StringPredicate lt(Object value) {
        return new StringPredicate("lt", value);
    }

    @Override
    public StringPredicate lte(Object value) {
        return new StringPredicate("lte", value);
    }

    @Override
    public StringPredicate neq(Object value) {
        return new StringPredicate("neq", value);
    }

    @Override
    public StringPredicate between(Object first, Object second) {
        return new StringPredicate("between", first, second);
    }

    @Override
    public StringPredicate within(Object... values) {
        return new StringPredicate("within", values);
    }

    @Override
    public StringPredicate without(Object... values) {
        return new StringPredicate("without", values);
    }

    @Override
    public StringPredicate startsWith(Object value) {
        return new StringPredicate("startsWith", value);
    }

    @Override
    public StringPredicate endsWith(Object value) {
        return new StringPredicate("endsWith", value);
    }

    @Override
    public StringPredicate contains(Object value) {
        return new StringPredicate("contains", value);
    }
}
