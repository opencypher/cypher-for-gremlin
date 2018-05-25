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

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import org.opencypher.gremlin.extension.CypherProcedure;
import org.opencypher.gremlin.extension.CypherProcedureProvider;

public final class GlobalProcedureContext {
    private GlobalProcedureContext() {
    }

    private static class LazyHolder {
        static final ProcedureContext context = load();

        private static ProcedureContext load() {
            ServiceLoader<CypherProcedureProvider> serviceLoader = ServiceLoader.load(CypherProcedureProvider.class);
            Set<CypherProcedure> procedures = new HashSet<>();
            for (CypherProcedureProvider provider : serviceLoader) {
                procedures.addAll(provider.get());
            }
            return new ProcedureContext(procedures);
        }
    }

    public static ProcedureContext get() {
        return LazyHolder.context;
    }

    public static CustomFunction procedureCall(String name) {
        return get().procedureCall(name);
    }
}
