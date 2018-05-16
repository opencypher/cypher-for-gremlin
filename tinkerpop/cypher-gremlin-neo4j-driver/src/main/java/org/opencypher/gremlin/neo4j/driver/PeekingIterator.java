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
package org.opencypher.gremlin.neo4j.driver;

import java.util.Iterator;

class PeekingIterator<E> implements Iterator<E> {
    private final Iterator<E> iterator;
    private E peeked = null;

    PeekingIterator(Iterator<E> iterator) {
        this.iterator = iterator;
    }

    private boolean hasPeeked() {
        return peeked != null;
    }

    @Override
    public boolean hasNext() {
        return hasPeeked() || iterator.hasNext();
    }

    @Override
    public E next() {
        if (!hasPeeked()) {
            return iterator.next();
        }
        E result = peeked;
        peeked = null;
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Removal from result iterator is not supported");
    }

    public E peek() {
        if (!hasPeeked()) {
            peeked = iterator.next();
        }
        return peeked;
    }
}
