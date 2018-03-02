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

public enum CustomPredicate implements BiPredicate<Object, Object> {
    startsWith {
        @Override
        public boolean test(Object a, Object b) {
            return a != null && b != null && a.toString().startsWith(b.toString());
        }
    },

    endsWith {
        @Override
        public boolean test(Object a, Object b) {
            return a != null && b != null && a.toString().endsWith(b.toString());
        }
    },

    contains {
        @Override
        public boolean test(Object a, Object b) {
            return a != null && b != null && a.toString().contains(b.toString());
        }
    };

    public static P<Object> startsWith(final Object prefix) {
        return new P<>(CustomPredicate.startsWith, prefix);
    }

    public static P<Object> endsWith(final Object suffix) {
        return new P<>(CustomPredicate.endsWith, suffix);
    }

    public static P<Object> contains(final Object sequence) {
        return new P<>(CustomPredicate.contains, sequence);
    }
}
