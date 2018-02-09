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

import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.opencypher.gremlin.traversal.ReturnNormalizer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

public final class ResultSetTransformer {
    private ResultSetTransformer() {
    }

    public static List<Map<String, Object>> resultSetAsMap(ResultSet resultSet) {
        return resultSet.all()
            .thenApply(ResultSetTransformer::resultsAsMap)
            .join();
    }

    public static CompletableFuture<List<Map<String, Object>>> resultSetAsMapAsync(
        CompletableFuture<ResultSet> resultSetFuture
    ) {
        return resultSetFuture
            .thenCompose(ResultSet::all)
            .thenApply(ResultSetTransformer::resultsAsMap);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> resultsAsMap(List<Result> results) {
        return results.stream()
            .map(result -> (Map<String, Object>) result.get(Map.class))
            .map(ReturnNormalizer::normalize)
            .collect(toList());
    }
}
