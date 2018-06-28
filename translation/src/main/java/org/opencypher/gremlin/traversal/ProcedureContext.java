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
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opencypher.gremlin.extension.CypherBinding;
import org.opencypher.gremlin.extension.CypherProcedure;

public final class ProcedureContext {

    private final Map<String, CypherProcedure> procedures;
    private final ReturnNormalizer returnNormalizer = ReturnNormalizer.create(emptyMap());

    private static final class LazyHolder {
        private static final ProcedureContext GLOBAL = new ProcedureContext(emptySet());
    }

    public static ProcedureContext global() {
        return LazyHolder.GLOBAL;
    }

    public static ProcedureContext empty() {
        return new ProcedureContext(emptySet());
    }

    public ProcedureContext(Set<CypherProcedure> procedures) {
        this.procedures = procedures.stream()
            .collect(Collectors.toMap(
                CypherProcedure::name,
                Function.identity()
            ));
    }

    public CypherProcedure findOrThrow(String name) {
        CypherProcedure procedure = procedures.get(name);
        if (procedure == null) {
            throw new IllegalArgumentException("Procedure not found: " + name);
        }
        return procedure;
    }

    public Set<CypherProcedure> all() {
        return new HashSet<>(procedures.values());
    }

    void unsafeClear() {
        procedures.clear();
    }

    void unsafeRegister(CypherProcedure procedure) {
        procedures.put(procedure.name(), procedure);
    }

    public CustomFunction procedureCall(String name) {
        return new CustomFunction(
            "procedureCall",
            traverser -> {
                Collection<?> arguments = (Collection<?>) traverser.get();
                return call(name, arguments);
            }
        );
    }

    private Object call(String name, Collection<?> arguments) {
        CypherProcedure procedure = findOrThrow(name);
        Object[] args = returnNormalizer.normalizeCollection(arguments).toArray();
        List<CypherBinding> defArgs = procedure.arguments();
        List<Object> callArgs = Arrays.asList(args);

        // Defined argument types
        List<Class<?>> defArgTypes = defArgs.stream()
            .map(CypherBinding::getType)
            .map(type -> Number.class.isAssignableFrom(type) ? Number.class : type)
            .collect(toList());

        // Argument types in call
        List<Class<?>> callArgTypes = new ArrayList<>();
        for (int i = 0; i < callArgs.size(); i++) {
            Object arg = callArgs.get(i);
            Class<?> type = arg != null ? arg.getClass() : defArgTypes.get(i);
            type = Number.class.isAssignableFrom(type) ? Number.class : type;
            callArgTypes.add(type);
        }

        // Validate signature
        int defArgsSize = defArgTypes.size();
        int callArgsSize = callArgTypes.size();
        if (defArgsSize != callArgsSize) {
            throw new IllegalArgumentException("Invalid number of arguments for " + name + ": " +
                defArgsSize + " expected, but " + callArgsSize + " provided");
        }
        if (!defArgTypes.equals(callArgTypes)) {
            throw new IllegalArgumentException("Invalid argument types for " + name + ": " +
                defArgTypes + " expected, but " + callArgTypes + " provided");
        }

        // Call implementation
        Map<String, Object> implArgs = new HashMap<>();
        for (int i = 0; i < defArgsSize; i++) {
            String argName = defArgs.get(i).getName();
            Class<?> argType = defArgs.get(i).getType();
            Object argValue = numericCast(callArgs.get(i), argType);
            implArgs.put(argName, argValue);
        }
        List<Map<String, Object>> rows = procedure.call(implArgs);

        // Reorder and normalize
        List<CypherBinding> defResults = procedure.results();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> orderedRow = new LinkedHashMap<>();
            for (CypherBinding res : defResults) {
                String resName = res.getName();
                Class<?> resType = res.getType();
                Object resValue = numericCast(row.get(resName), resType);
                orderedRow.put(resName, resValue);
            }
            results.add(returnNormalizer.normalize(orderedRow));
        }
        return results;
    }

    private static Object numericCast(Object value, Class<?> type) {
        if (value instanceof Number) {
            Number number = (Number) value;
            if (type.equals(Long.class)) {
                return number.longValue();
            }
            if (type.equals(Double.class)) {
                return number.doubleValue();
            }
        }
        return value;
    }
}
