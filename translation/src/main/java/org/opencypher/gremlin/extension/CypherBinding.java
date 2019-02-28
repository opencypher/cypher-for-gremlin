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
package org.opencypher.gremlin.extension;

public final class CypherBinding {
    private final String name;
    private final CypherBindingType type;

    public CypherBinding(String name, CypherBindingType type) {
        this.name = name;
        this.type = type;
    }

    public static CypherBinding binding(String name, CypherBindingType type) {
        return new CypherBinding(name, type);
    }

    public String getName() {
        return name;
    }

    public CypherBindingType getType() {
        return type;
    }
}
