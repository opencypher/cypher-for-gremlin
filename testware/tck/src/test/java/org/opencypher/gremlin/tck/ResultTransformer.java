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
package org.opencypher.gremlin.tck;


import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import org.apache.tinkerpop.gremlin.driver.ResultSet;

final class ResultTransformer {
    private ResultTransformer() {
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> resultSetAsMaps(ResultSet resultSet) {
        return resultSet.all()
            .join()
            .stream()
            .map(result -> (Map<String, Object>) result.get(Map.class))
            .collect(toList());
    }
}
