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

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.opencypher.gremlin.extension.CypherProcedure;
import org.opencypher.gremlin.extension.CypherProcedureProvider;

public class ProcedureRegistry {
    private static final Map<String, CypherProcedure> procedures = new HashMap<>();
    private static final ReturnNormalizer returnNormalizer = ReturnNormalizer.create(emptyMap());

    private ProcedureRegistry() {
    }

    public static void load() {
        ServiceLoader<CypherProcedureProvider> serviceLoader = ServiceLoader.load(CypherProcedureProvider.class);
        for (CypherProcedureProvider provider : serviceLoader) {
            provider.init(procedures::put);
        }
    }

    public static CustomFunction procedureCall(String name) {
        return new CustomFunction(
            "procedureCall",
            traverser -> {
                CypherProcedure implementation = procedures.get(name);
                if (implementation == null) {
                    throw new IllegalArgumentException("Procedure not found: " + name);
                }

                Collection<?> value = (Collection<?>) traverser.get();
                Collection<?> args = returnNormalizer.normalizeCollection(value);
                Map<String, Object> result = implementation.call(args);
                return ParameterNormalizer.normalize(result);
            },
            name
        );
    }
}
