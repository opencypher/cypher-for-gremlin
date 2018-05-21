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
package org.opencypher.gremlin.tck;

import static java.util.stream.Collectors.toList;
import static org.opencypher.gremlin.extension.CypherArgument.argument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opencypher.gremlin.extension.CypherArgument;
import org.opencypher.gremlin.traversal.ProcedureRegistry;

public final class PredefinedProcedureRegistry {
    private PredefinedProcedureRegistry() {
    }

    private static final Pattern SIGNATURE_PATTERN =
        Pattern.compile("^(?<name>[\\w.]+)\\((?<arguments>[^)]*)\\) :: (?:\\((?<results>[^)]*)\\)|VOID)");
    private static final Pattern ARGUMENT_PATTERN =
        Pattern.compile("(?<name>\\w+) :: (?<type>[\\w?]+)");

    public static void register(String signature, List<String> header, List<Map<String, Object>> rows) {
        Matcher signatureMatcher = SIGNATURE_PATTERN.matcher(signature);
        if (!signatureMatcher.find()) {
            throw new IllegalArgumentException("Unparsable procedure signature: " + signature);
        }
        String name = signatureMatcher.group("name");
        List<CypherArgument> arguments = matchArguments(signatureMatcher.group("arguments"));
        List<CypherArgument> results = matchArguments(signatureMatcher.group("results"));

        ProcedureRegistry.register(registry -> registry.register(
            name,
            arguments,
            results,
            args -> {
                List<String> in = header.subList(0, arguments.size());
                List<String> out = header.subList(arguments.size(), header.size());
                return rows.stream()
                    .filter(row -> extractKeys(in, row).equals(args))
                    .map(row -> extractKeys(out, row))
                    .collect(toList());
            }));
    }

    private static List<CypherArgument> matchArguments(String input) {
        List<CypherArgument> arguments = new ArrayList<>();
        if (input == null) {
            return arguments;
        }
        Matcher matcher = ARGUMENT_PATTERN.matcher(input);
        while (matcher.find()) {
            String name = matcher.group("name");
            Class<?> type = typeFromSignature(matcher.group("type"));
            arguments.add(argument(name, type));
        }
        return arguments;
    }

    private static Class<?> typeFromSignature(String type) {
        switch (type) {
            case "FLOAT?":
                return Double.class;
            case "INTEGER?":
                return Long.class;
            case "NUMBER?":
                return Number.class;
            case "STRING?":
                return String.class;
            default:
                throw new IllegalArgumentException("Unparsable procedure type: " + type);
        }
    }

    private static Map<String, Object> extractKeys(List<String> keys, Map<String, Object> src) {
        Map<String, Object> dest = new HashMap<>();
        for (String key : keys) {
            dest.put(key, src.get(key));
        }
        return dest;
    }
}
