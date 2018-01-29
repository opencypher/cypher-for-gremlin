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
package org.opencypher.gremlin.translation;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to create and track multiple Gremlin aliases for a single Cypher variable.
 * Multiple aliases are used in translation of Cypher variables re-use and shadowing.
 */
public class AliasHistory {

    private final Map<String, Integer> aliases;

    public AliasHistory() {
        this(new HashMap<>());
    }

    private AliasHistory(Map<String, Integer> aliases) {
        this.aliases = aliases;
    }

    public AliasHistory copy() {
        return new AliasHistory(new HashMap<>(aliases));
    }

    public String next(String alias) {
        int index = aliases.compute(alias, (key, value) -> value == null ? 0 : value + 1);
        return index == 0 ? alias : alias + "_" + index;
    }

    public String current(String alias) {
        int index = aliases.getOrDefault(alias, 0);
        return index == 0 ? alias : alias + "_" + index;
    }

}
