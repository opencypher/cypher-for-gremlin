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
package org.opencypher.gremlin.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CypherProcedureDefinition {
    private final Map<String, CypherProcedureSignature> signatures;
    private final Map<String, CypherProcedure> implementations;

    public CypherProcedureDefinition() {
        signatures = new HashMap<>();
        implementations = new HashMap<>();
    }

    public void define(String name, List<CypherBinding> arguments, List<CypherBinding> results, CypherProcedure implementation
    ) {
        signatures.put(name, new CypherProcedureSignature(arguments, results));
        implementations.put(name, implementation);
    }

    public Map<String, CypherProcedureSignature> getSignatures() {
        return signatures;
    }

    public Map<String, CypherProcedure> getImplementations() {
        return implementations;
    }
}
