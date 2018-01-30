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

import org.apache.tinkerpop.gremlin.structure.Column;
import org.opencypher.gremlin.translation.TranslationBuilder;
import org.opencypher.gremlin.translation.string.StringPredicate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.opencypher.gremlin.traversal.ReturnNormalizer.normalize;

public class GremlinRemote {
    public static final String PIVOTS = "pivots";
    public static final String AGGREGATIONS = "aggregations";

    public static Map<String, Object> normalizeRow(Map<String, Object> fromRemote) {
        HashMap<String, Object> result = new LinkedHashMap<>();
        if (fromRemote.containsKey(PIVOTS) && fromRemote.containsKey(AGGREGATIONS)) {
            if (fromRemote.get(PIVOTS) instanceof Map) {
                result.putAll((Map<? extends String, ?>) normalize(fromRemote.get(PIVOTS)));
            }
            if (fromRemote.get(AGGREGATIONS) instanceof Map) {
                result.putAll((Map<? extends String, ?>) normalize(fromRemote.get(AGGREGATIONS)));
            }
        } else {
            result.putAll(normalize(fromRemote));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> normalizeResults(List<Map<String, Object>> results) {
        return results.stream()
            .map(GremlinRemote::normalizeRow)
            .collect(toList());
    }

    public static Function<TranslationBuilder<String, StringPredicate>, String> transposeReturnMap() {
        return builder -> builder.project(PIVOTS, AGGREGATIONS)
            .by(builder.start().select(Column.keys))
            .by(builder.start().select(Column.values))
            .current();
    }
}
