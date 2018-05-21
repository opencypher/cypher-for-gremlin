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
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.opencypher.gremlin.extension.CypherArgument;
import org.opencypher.gremlin.extension.CypherProcedure;
import org.opencypher.gremlin.extension.CypherProcedureProvider;

/**
 * Facility to manage user-defined procedures that can be called with the {@code CALL} keyword.
 */
public class ProcedureRegistry {
    private static final Map<String, ProcedureDefinition> procedures = new HashMap<>();
    private static final ReturnNormalizer returnNormalizer = ReturnNormalizer.create(emptyMap());

    private ProcedureRegistry() {
    }

    /**
     * Removes all currently-registered procedures.
     */
    public static void clear() {
        procedures.clear();
    }

    /**
     * Loads all procedures that are exposed via SPI.
     *
     * @see CypherProcedureProvider
     */
    public static void load() {
        ServiceLoader<CypherProcedureProvider> serviceLoader = ServiceLoader.load(CypherProcedureProvider.class);
        for (CypherProcedureProvider provider : serviceLoader) {
            register(provider);
        }
    }

    /**
     * Explicitly registers provided procedures.
     *
     * @param provider procedure provider
     */
    public static void register(CypherProcedureProvider provider) {
        provider.apply((name, arguments, results, implementation) ->
            procedures.put(name, new ProcedureDefinition(arguments, results, implementation)));
    }

    public static ProcedureDefinition getDefinition(String name) {
        return procedures.get(name);
    }

    public static CustomFunction procedureCall(String name) {
        return new CustomFunction(
            "procedureCall",
            traverser -> {
                ProcedureDefinition definition = procedures.get(name);
                Collection<?> value = (Collection<?>) traverser.get();
                Object[] args = returnNormalizer.normalizeCollection(value).toArray();
                List<CypherArgument> defArgs = definition.getArguments();
                List<Object> callArgs = Arrays.asList(args);

                // Defined argument types
                List<Class<?>> defArgTypes = defArgs.stream()
                    .map(CypherArgument::getType)
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
                CypherProcedure implementation = definition.getImplementation();
                Map<String, Object> implArgs = new HashMap<>();
                for (int i = 0; i < defArgsSize; i++) {
                    String argName = defArgs.get(i).getName();
                    Class<?> argType = defArgs.get(i).getType();
                    Object argValue = numericCast(callArgs.get(i), argType);
                    implArgs.put(argName, argValue);
                }
                List<Map<String, Object>> rows = implementation.call(implArgs);

                // Reorder and normalize
                List<CypherArgument> defResults = definition.getResults();
                List<Map<String, Object>> results = new ArrayList<>();
                for (Map<String, Object> row : rows) {
                    Map<String, Object> orderedRow = new LinkedHashMap<>();
                    for (CypherArgument res : defResults) {
                        String resName = res.getName();
                        Class<?> resType = res.getType();
                        Object resValue = numericCast(row.get(resName), resType);
                        orderedRow.put(resName, resValue);
                    }
                    results.add(returnNormalizer.normalize(orderedRow));
                }
                return results;
            },
            name
        );
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
