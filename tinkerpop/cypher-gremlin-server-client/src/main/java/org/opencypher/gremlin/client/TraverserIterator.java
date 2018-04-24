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
package org.opencypher.gremlin.client;

import java.util.Iterator;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.EmptyTraverser;

/**
 * This is an iterator adapter that unwraps bulked traversers
 * and iterates over each bulked value the specified number of times.
 *
 * @see org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet
 */
public class TraverserIterator implements Iterator<Result> {

    private final Iterator<Result> iterator;
    private Traverser lastTraverser = EmptyTraverser.instance();

    TraverserIterator(Iterator<Result> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return lastTraverser.bulk() > 0 || iterator.hasNext();
    }

    @Override
    public Result next() {
        if (lastTraverser.bulk() == 0) {
            lastTraverser = (Traverser) iterator.next().getObject();
        }
        Result result = new Result(lastTraverser.get());
        if (lastTraverser.bulk() > 1) {
            lastTraverser.asAdmin().setBulk(lastTraverser.bulk() - 1);
        } else {
            lastTraverser = EmptyTraverser.instance();
        }
        return result;
    }
}
