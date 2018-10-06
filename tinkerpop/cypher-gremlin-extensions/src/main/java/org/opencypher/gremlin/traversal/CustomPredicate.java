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

import java.util.function.BiPredicate;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.Vertex;


public enum CustomPredicate implements BiPredicate<Object, Object> {
    cypherStartsWith {
        @Override
        public boolean test(Object a, Object b) {
            return a != null && b != null && a.toString().startsWith(b.toString());
        }
    },

    cypherEndsWith {
        @Override
        public boolean test(Object a, Object b) {
            return a != null && b != null && a.toString().endsWith(b.toString());
        }
    },

    cypherContains {
        @Override
        public boolean test(Object a, Object b) {
            return a != null && b != null && a.toString().contains(b.toString());
        }
    },

    cypherIsNode {
        @Override
        public boolean test(Object a, Object b) {
            return a instanceof Vertex;
        }
    },

    cypherIsString {
        @Override
        public boolean test(Object a, Object b) {
            return a instanceof String;
        }
    };

    public static P<Object> cypherStartsWith(final Object prefix) {
        return new P<>(CustomPredicate.cypherStartsWith, prefix);
    }

    public static P<Object> cypherEndsWith(final Object suffix) {
        return new P<>(CustomPredicate.cypherEndsWith, suffix);
    }

    public static P<Object> cypherContains(final Object sequence) {
        return new P<>(CustomPredicate.cypherContains, sequence);
    }

    public static P<Object> cypherIsNode() {
        return new P<>(CustomPredicate.cypherIsNode, null);
    }

    public static P<Object> cypherIsString() {
        return new P<>(CustomPredicate.cypherIsString, null);
    }
}
