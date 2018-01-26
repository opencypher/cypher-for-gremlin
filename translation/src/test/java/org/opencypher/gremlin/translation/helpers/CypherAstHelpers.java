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
package org.opencypher.gremlin.translation.helpers;

import org.opencypher.gremlin.translation.CypherAstWrapper;

import java.util.Map;

import static java.util.Collections.emptyMap;

public final class CypherAstHelpers {
    private CypherAstHelpers() {
    }

    public static CypherAstWrapper parse(String queryText) {
        return parse(queryText, emptyMap());
    }

    public static CypherAstWrapper parse(String queryText, Map<String, Object> passedParams) {
        try {
            return CypherAstWrapper.parse(queryText, passedParams);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing query: " + queryText, e);
        }
    }
}
