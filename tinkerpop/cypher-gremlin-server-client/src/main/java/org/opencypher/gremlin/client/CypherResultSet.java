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

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

/**
 * A Gremlin query result iterator wrapper.
 * <p>
 * In the case of most {@link CypherGremlinClient} configurations,
 * instances of this class wrap a Gremlin {@link org.apache.tinkerpop.gremlin.driver.ResultSet} iterator,
 * so this class is not thread-safe, by extension.
 *
 * @see CypherGremlinClient
 */
public final class CypherResultSet implements Iterable<Map<String, Object>> {

    private final Iterator<Result> resultIterator;

    CypherResultSet(Iterator<Result> resultIterator) {
        this.resultIterator = resultIterator;
    }

    /**
     * Waits for all query results to be received by the client and collects them in a list.
     *
     * @return list of all query results
     */
    public List<Map<String, Object>> all() {
        return stream().collect(toList());
    }

    /**
     * Creates a spliterator over query results.
     *
     * @return a spliterator over query results
     */
    @Override
    public Spliterator<Map<String, Object>> spliterator() {
        return spliteratorUnknownSize(iterator(), NONNULL | IMMUTABLE);
    }

    /**
     * Returns a sequential stream of query results.
     *
     * @return a sequential stream of query results
     */
    public Stream<Map<String, Object>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Returns a blocking iterator of the items streaming from the server to the client.
     *
     * @return query results iterator
     */
    @Override
    public Iterator<Map<String, Object>> iterator() {
        return new Iterator<Map<String, Object>>() {
            @Override
            public boolean hasNext() {
                return resultIterator.hasNext();
            }

            @Override
            public Map<String, Object> next() {
                Result result = resultIterator.next();
                Object row = result.getObject();
                return ReturnNormalizer.normalize(row);
            }
        };
    }
}
