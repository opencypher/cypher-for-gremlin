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

import static java.util.Collections.unmodifiableList;

import java.util.List;
import org.opencypher.gremlin.extension.CypherArgument;
import org.opencypher.gremlin.extension.CypherProcedure;

public final class ProcedureDefinition {
    private final List<CypherArgument> arguments;
    private final List<CypherArgument> results;
    private final CypherProcedure implementation;

    ProcedureDefinition(List<CypherArgument> arguments,
                        List<CypherArgument> results,
                        CypherProcedure implementation) {
        this.arguments = unmodifiableList(arguments);
        this.results = unmodifiableList(results);
        this.implementation = implementation;
    }

    public List<CypherArgument> getArguments() {
        return arguments;
    }

    public List<CypherArgument> getResults() {
        return results;
    }

    public CypherProcedure getImplementation() {
        return implementation;
    }
}
